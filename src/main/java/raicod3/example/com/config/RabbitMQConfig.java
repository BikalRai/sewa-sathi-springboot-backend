package raicod3.example.com.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String EMAIL_QUEUE = "queue.email.notification";
    public static final String EMAIL_DLQ = "queue.email.dlq";
    public static final String JOB_ANALYSIS_QUEUE = "queue.job.analysis";
    public static final String CHAT_QUEUE = "queue.chat";

    // Routing keys - labels on the envelope
    public static final String EMAIL_ROUTING_KEY="email.notification";
    public static final String EMAIL_DLQ_ROUTING_KEY="email.dlq";
    public static final String JOB_ANALYSIS_ROUTING_KEY="job.analysis";
    public static final String CHAT_ROUTING_KEY="chat.message";

    // One exchante to rule them all
    public static final String EXCHANGE = "app.direct.exchange";
    public static final String DLQ_EXCHANGE = "app.direct.exchange";

    public static final String PROVIDER_MATCH_QUEUE = "provider.match.queue";
    public static final String PROVIDER_MATCH_EXCHANGE = "provider.match.exchange";
    public static final String PROVIDER_MATCH_ROUTING_KEY = "provider.match";

    // Dead Letter Queue — the "failed mail" bin
    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    // Queues
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).withArgument("x-dead-letter-exchange", DLQ_EXCHANGE).withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Queue jobAnalysisQueue() {
        return QueueBuilder.durable(JOB_ANALYSIS_QUEUE).build();
    }

    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(CHAT_QUEUE).build();
    }

    // Exchange
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    // Bindings - connects queue to exchange via routing key
    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(exchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding jobAnalysisBinding() {
        return BindingBuilder.bind(jobAnalysisQueue()).to(exchange()).with(JOB_ANALYSIS_ROUTING_KEY);
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue()).to(exchange()).with(CHAT_ROUTING_KEY);
    }

    // Converts objects to JSON automatically when sending/receiving messages
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // Wires the JSON converter into the RabbitMQ template
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Queue providerMatchQueue() {
        return new Queue(PROVIDER_MATCH_QUEUE, true);
    }

    @Bean
    public DirectExchange providerMatchExchange() {
        return new DirectExchange(PROVIDER_MATCH_EXCHANGE);
    }

    @Bean
    public Binding providerMatchBinding() {
        return BindingBuilder.bind(providerMatchQueue())
                .to(providerMatchExchange())
                .with(PROVIDER_MATCH_ROUTING_KEY);
    }
}
