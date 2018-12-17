package cr.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cr.generated.interf.Listener;
import cr.generated.ops.MessageListener;
import cr.interf.EncryptedMessage;
import cr.service.BuilderOperator;

@Configuration
public class ApplicationListener {
	@Autowired
	private BuilderOperator buo;
	
	@Bean
	public Listener listener() {
		return new Listener() {
			@Override
			public void processBui(EncryptedMessage message) {
				// TODO Auto-generated method stub
				buo.action(message);
			}
//			@Override
//			public boolean isProcessing() {
//				// TODO Auto-generated method stub
//				return processing;
//			}
//			@Override
//			public void setProcessing(boolean processing) {
//				// TODO Auto-generated method stub
//				this.processing=processing;
//			}
			
		};
	}
	
	@Bean
	public MessageListener messageListener() {
		return new MessageListener() ;
	}
}

