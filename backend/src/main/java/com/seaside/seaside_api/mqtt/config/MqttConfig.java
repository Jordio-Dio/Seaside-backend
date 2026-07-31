package com.seaside.seaside_api.mqtt.config;

import com.seaside.seaside_api.mqtt.router.MqttTelemetryRouter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.Arrays;

@Slf4j
@Configuration
public class MqttConfig {


@Value("${mqtt.broker.url}")
private String brokerUrl;

@Value("${mqtt.client.id}")
private String clientId;

// Exemple :
// mqtt.topic=seaside/telemetry/master,seaside/telemetry/slave/#
@Value("${mqtt.topic}")
private String topic;

@Value("${mqtt.username:}")
private String username;

@Value("${mqtt.password:}")
private String password;

@Bean
public MqttPahoClientFactory mqttClientFactory() {

    DefaultMqttPahoClientFactory factory =
            new DefaultMqttPahoClientFactory();

    MqttConnectOptions options =
            new MqttConnectOptions();

    options.setServerURIs(new String[]{brokerUrl});
    options.setCleanSession(true);
    options.setAutomaticReconnect(true);
    options.setKeepAliveInterval(60);

    if (username != null && !username.isEmpty()) {
        options.setUserName(username);
        options.setPassword(password.toCharArray());
    }

    log.info("Connexion MQTT → {}", brokerUrl);

    factory.setConnectionOptions(options);

    return factory;
}

@Bean
public MessageChannel mqttInputChannel() {
    return new DirectChannel();
}

@Bean
public MqttPahoMessageDrivenChannelAdapter mqttAdapter() {

    String[] topics = topic.split(",");

    log.info("Abonnement MQTT : {}",
            String.join(" | ", topics));

    MqttPahoMessageDrivenChannelAdapter adapter =
            new MqttPahoMessageDrivenChannelAdapter(
                    clientId + "-inbound",
                    mqttClientFactory(),
                    topics
            );

    adapter.setCompletionTimeout(5000);
    adapter.setConverter(new DefaultPahoMessageConverter());

    // QoS = 1 pour chaque topic
    int[] qos = new int[topics.length];
    Arrays.fill(qos, 1);
    adapter.setQos(qos);

    adapter.setOutputChannel(mqttInputChannel());

    return adapter;
}

@Bean
@ServiceActivator(inputChannel = "mqttInputChannel")
public MessageHandler mqttMessageHandler(
        MqttTelemetryRouter telemetryRouter) {

    return message -> {

        String topic =
                (String) message.getHeaders()
                        .get("mqtt_receivedTopic");

        String payload =
                (String) message.getPayload();

        log.info("MQTT reçu → topic: {} | payload: {}",
                topic,
                payload);

        telemetryRouter.router(topic, payload);
    };
}

@Bean
public MqttPahoMessageHandler mqttOutbound() {

    MqttPahoMessageHandler handler =
            new MqttPahoMessageHandler(
                    clientId + "-outbound",
                    mqttClientFactory()
            );

    handler.setAsync(true);
    handler.setDefaultQos(1);
    handler.setDefaultRetained(false);

    return handler;
}

}
