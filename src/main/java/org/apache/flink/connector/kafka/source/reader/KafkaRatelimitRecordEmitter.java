package org.apache.flink.connector.kafka.source.reader;

import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;
import org.apache.flink.shaded.guava30.com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaRatelimitRecordEmitter<T> implements RecordEmitter<Tuple3<T, Long, Long>, T, KafkaPartitionSplitState>{
    protected static final Logger LOG = LoggerFactory.getLogger(KafkaRatelimitRecordEmitter.class);
    private transient RateLimiter rateLimiter;
    
    public KafkaRatelimitRecordEmitter(double rateLimit) {
        LOG.debug("constructor record emitter");
        this.rateLimiter = RateLimiter.create(rateLimit);
    }

    @Override
    public void emitRecord(
            Tuple3<T, Long, Long> element,
            SourceOutput<T> output,
            KafkaPartitionSplitState splitState)
            throws Exception {
        
        this.rateLimiter.acquire();
        output.collect(element.f0, element.f2);
        splitState.setCurrentOffset(element.f1 + 1);
    }
}
