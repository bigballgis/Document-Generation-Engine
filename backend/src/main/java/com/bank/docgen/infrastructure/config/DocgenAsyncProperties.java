package com.bank.docgen.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.async")
public class DocgenAsyncProperties {

    /**
     * Async batch transport: {@code in-process} (default) or {@code kafka}.
     */
    private String transport = "in-process";

    private Kafka kafka = new Kafka();

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public static class Kafka {

        private String asyncBatchTopic = "generation.async-batch-task.v1";

        private String deadLetterTopic = "generation.async-batch-task.v1.dlt";

        private String consumerGroup = "docgen-async-batch-worker";

        public String getAsyncBatchTopic() {
            return asyncBatchTopic;
        }

        public void setAsyncBatchTopic(String asyncBatchTopic) {
            this.asyncBatchTopic = asyncBatchTopic;
        }

        public String getDeadLetterTopic() {
            return deadLetterTopic;
        }

        public void setDeadLetterTopic(String deadLetterTopic) {
            this.deadLetterTopic = deadLetterTopic;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }
    }
}
