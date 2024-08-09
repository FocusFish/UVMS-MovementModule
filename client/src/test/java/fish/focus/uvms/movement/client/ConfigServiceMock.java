package fish.focus.uvms.movement.client;

import fish.focus.schema.config.types.v1.PullSettingsStatus;
import fish.focus.schema.config.types.v1.SettingType;
import fish.focus.uvms.config.model.mapper.ModuleResponseMapper;
import fish.focus.uvms.movement.service.message.MovementProducer;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Arrays;

@MessageDriven(mappedName = "jms/queue/UVMSConfigEvent", activationConfig = {
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "UVMSConfigEvent")})
public class ConfigServiceMock implements MessageListener {

    @EJB
    private MovementProducer producer;

    @Override
    public void onMessage(Message message) {
        try {
            SettingType endpointSetting = new SettingType();
            endpointSetting.setKey("APA");
            endpointSetting.setValue("BEPA");
            endpointSetting.setDescription("From ConfigServiceMock.java");
            String response = ModuleResponseMapper.toPullSettingsResponse(Arrays.asList(endpointSetting), PullSettingsStatus.OK);
            producer.sendResponseMessageToSender((TextMessage) message, response);
        } catch (JMSException e) {
        }
    }
}
