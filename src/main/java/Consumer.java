import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Consumer {

    private final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());
    private final String mBootStrapServer;
    private String mGroupId;
    private String mTopic;

    Consumer(String bootStrapServer, String mGroupId, String topic){
        mBootStrapServer = bootStrapServer;
        this.mGroupId = mGroupId;
        this.mTopic = topic;
    }

    public static void main(String[] args) {
        String server = "127.0.0.1:9092";
        String groupId = "some_application";
        String topic = "user_registered";

        new Consumer(server, groupId, topic).run();
    }

    void run() {
        mLogger.info("Creating consumer thread");

        CountDownLatch latch = new CountDownLatch(1);

        ConsumerRunnable consumerRunnable = new ConsumerRunnable(mBootStrapServer, mGroupId, mTopic, latch);
        Thread thread = new Thread(consumerRunnable);
        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mLogger.info("Caught shutdown hook");
            consumerRunnable.shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mLogger.info("Application has exited");
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ConsumerRunnable implements Runnable {

        private CountDownLatch mLatch;
        private KafkaConsumer<String, String> mConsumer;

        ConsumerRunnable(String bootstrapServer, String groupId, String topic, CountDownLatch latch) {
            mLatch = latch;
            Properties props = consumerProps(bootstrapServer, groupId);
            mConsumer = new KafkaConsumer<String, String>(props);
            mConsumer.subscribe(Collections.singleton(topic));

        }

        private Properties consumerProps(String bootStrapServer, String groupId) {
            String deserliazer = StringDeserializer.class.getName();
            Properties props = new Properties();
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
            props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserliazer);
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserliazer);
            props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


            return props;
        }

        @Override
        public void run() {
            try {
                do {
                    ConsumerRecords<String, String> records = mConsumer.poll(Duration.ofMillis(100));

                    for(ConsumerRecord<String, String> record: records) {
                        mLogger.info("Key: " + record.key() + ", Value: " + record.value());
                        mLogger.info("Partition " + record.partition() + ", Offset: " + record.offset());
                    }
                } while (true);
            } catch(WakeupException e) {
                mLogger.info("Received shutdown signal!");
            } finally {
                mConsumer.close();
                mLatch.countDown();
            }
        }

        void shutdown() {
            mConsumer.wakeup();
        }
    }
}
