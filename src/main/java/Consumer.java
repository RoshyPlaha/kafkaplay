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

        final int THREAD_ONE = 1;
        final int THREAD_TWO = 2;

        Consumer consumer = new Consumer(server, groupId, topic);
        consumer.spawn(THREAD_TWO);
        System.out.println("Second thread time");
        consumer.spawn(THREAD_ONE);
    }

    void spawn(int threadNumber) {
        mLogger.info("Creating consumer thread [" + threadNumber + "]");

        ConsumerRunnable consumerRunnable = new ConsumerRunnable(mBootStrapServer, mGroupId, mTopic, threadNumber);
        consumerRunnable.start();
        ConsumerRunnable consumerRunnable2 = new ConsumerRunnable(mBootStrapServer, mGroupId, mTopic, threadNumber);
        consumerRunnable2.start();


    }

    private class ConsumerRunnable extends Thread {

        private KafkaConsumer<String, String> mConsumer;
        private int mThreadNumber;

        ConsumerRunnable(String bootstrapServer, String groupId, String topic, int threadNumber) {
            Properties props = consumerProps(bootstrapServer, groupId);
            mConsumer = new KafkaConsumer<String, String>(props);
            mConsumer.subscribe(Collections.singleton(topic));
            mThreadNumber = threadNumber;

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
                        mLogger.info(mThreadNumber + " Key: " + record.key() + ", Value: " + record.value());
                        mLogger.info(mThreadNumber + "Partition " + record.partition() + ", Offset: " + record.offset());
                    }
                } while (true);
            } catch(WakeupException e) {
                mLogger.info(mThreadNumber + "Received shutdown signal!");
            } finally {
                mConsumer.close();
            }
        }

    }
}
