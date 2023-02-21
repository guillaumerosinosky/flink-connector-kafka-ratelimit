package org.apache.flink.connector.kafka.source.reader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;

import org.apache.flink.shaded.guava30.com.google.common.util.concurrent.RateLimiter;
import org.apache.flink.table.data.GenericRowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaRatelimitRecordEmitter<T>
        implements RecordEmitter<Tuple3<T, Long, Long>, T, KafkaPartitionSplitState> {
    protected static final Logger LOG = LoggerFactory.getLogger(KafkaRatelimitRecordEmitter.class);
    private transient RateLimiter rateLimiter;
    private ScheduledExecutorService ses;

    public KafkaRatelimitRecordEmitter(double rateLimit) {
        //LOG.info("constructor record emitter");
        this.rateLimiter = RateLimiter.create(rateLimit);
        ses = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void emitRecord(
            Tuple3<T, Long, Long> element,
            SourceOutput<T> output,
            KafkaPartitionSplitState splitState)
            throws Exception {
        
        if (splitState.getTopic().contains("control")) {
            GenericRowData grd = (GenericRowData) element.f0;// TODO: quick and dirty: make it specific 
            double rate = grd.getDouble(0);
            if (grd.getField(1) != null) {
                Long timestamp = grd.getLong(1);
                Long now = System.currentTimeMillis();
                
                LOG.info(String.format("Plan control event to %d (now=%d), in %d ms : %s (%s)", timestamp, now, timestamp-now, grd.toString(), splitState.toString()));
                rateLimiter = this.rateLimiter;
                ses.schedule( new Runnable() {
                    public void run() {                      
                        LOG.info(String.format("Apply planned control event %s (%s) ", grd.toString(), splitState.toString()));
                        rateLimiter.setRate(rate);
                    }
                }, timestamp - now, TimeUnit.MILLISECONDS);
            } else {
                LOG.info(String.format("Apply control event %s (%s) ", grd.toString(), splitState.toString()));
                this.rateLimiter.setRate(rate);
            }
        } else {
            this.rateLimiter.acquire();
            output.collect(element.f0, element.f2);
            splitState.setCurrentOffset(element.f1 + 1);
        }
    }
}
