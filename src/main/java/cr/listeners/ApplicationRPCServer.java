package cr.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



import cr.generated.interf.ProcessRPCResponse;
import cr.interf.EncryptedMessage;
import cr.service.BuilderOperator;


@Configuration
public class ApplicationRPCServer {
	@Autowired
	private BuilderOperator bo;
	@Bean
	public ProcessRPCResponse ProcessResponse() {
		return new ProcessRPCResponse() {
			@Override
			public EncryptedMessage processBuiResponseRPC(EncryptedMessage message) {
				return bo.action(message);
			}
		};
	}
}
