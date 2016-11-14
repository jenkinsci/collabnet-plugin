package hudson.plugins.collabnet.actionhub;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import lombok.extern.java.Log;

@Log
public class MQConnectionHandler implements ShutdownListener {

    @Override
    public void shutdownCompleted(ShutdownSignalException cause) {
        boolean reConnectSuccess = true;
        int count = 0;
        log.info("Lost connection to RabbitMQ");

        do {
            int timeInterval = Constants.RABBIT_CONNECTION_RETRY_INTERVALS[count];

            try {
                TimeUnit.MINUTES.sleep(timeInterval);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            log.info("Been " + timeInterval + " minute(s). Going to retry.");
            reConnectSuccess = true; //assume try succeeded

            try {
                ActionHubPlugin.connectAndListen();
            } catch (IOException ioe) {
                reConnectSuccess = false;
            } catch (TimeoutException te) {
                reConnectSuccess = false;
            }

            count++;
        } while (!reConnectSuccess && count < Constants.RABBIT_CONNECTION_RETRY_INTERVALS.length);

        if (!reConnectSuccess) {
            log.info("Unable to reconnect to Rabbit. Giving up.");
        }

    }
}
