package cr.service;

import java.io.IOException;
import java.util.StringTokenizer;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import cr.ProjectBuilderApplication;
import cr.generated.ops.service.RPCClient;
import cr.interf.EncryptedMessage;
import cr.shared.BuildRecord;
import cr.shared.JenkinsJob;
import cr.shared.Operation;
import cr.shared.SecurityProject;
import cr.util.JenkinsAdapter;
import cr.util.RepoHandler;

@RefreshScope
@Service
public class BuilderOperator {

	@Autowired
	private RPCClient client;
	@Autowired
	private RabbitListenerEndpointRegistry listenersreg;
	private static String encryption = ProjectBuilderApplication.class
			.getAnnotation(cr.annotation.QueueDefinition.class).encryption();
	private static Logger log = LoggerFactory.getLogger(BuilderOperator.class);

	@Value("${projects.storage}")
	private String storagePath;
	@Value("${repo.user}")
	private String repoUsername;
	@Value("${repo.passwd}")
	private String repoPasswd;

	public synchronized EncryptedMessage action(EncryptedMessage message) {
		if (message.getPayloadType().equals("cr.shared.Operation"))
			return processOperation(message);
		if (message.getPayloadType().equals("cr.shared.BuildRecord"))
			return manageBuild(message);
		Operation nop = new Operation("NOP");
		return nop.toEncryptedMessage(encryption).encodeBase64();
	}

	private EncryptedMessage manageBuild(EncryptedMessage message) {
		BuildRecord br=message.decodeBase64ToObject();
		SecurityProject sp=new SecurityProject();
		sp.setId(br.getIdSecurityProject());
		sp=(SecurityProject) (client.sendAndReceiveDb(sp.toEncryptedMessage(encryption).encodeBase64()).decodeBase64ToObject());
		br.setStatus("BUILDSTARTING");
		client.sendAndReceiveDb(br.toEncryptedMessage(encryption).encodeBase64());
		RepoHandler rh=new RepoHandler();
		StringTokenizer st=new StringTokenizer(sp.getUrl(), "//");
		String protocol=st.nextToken();
		String address="";
		String project=null;
		int i=0;
		while(st.hasMoreTokens()) {
			if(i>0)
				address+="/";
			project=st.nextToken();
			address+=project;
			i++;
		}
		project=project.replace(".git", "");
		String completeUrl=protocol+"//"+repoUsername+"@"+address;
		try {
			rh.rebaseRemoteBuffer(br.getStoragefolder(), completeUrl, repoUsername, repoPasswd);
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}
		System.out.println("Project "+br.getIdRepository()+" pushed to security repository "+sp.getUrl());
		JenkinsJob jj=new JenkinsJob();
		jj.setIdSecurityRepository(br.getIdSecurityProject());
		EncryptedMessage enc=client.sendAndReceiveDb(jj.toEncryptedMessage(encryption).encodeBase64());
		jj=enc.decodeBase64ToObject();
		JenkinsAdapter ja=new JenkinsAdapter(jj.getUser(), jj.getToken(), jj.getUrl());
		ja.buildJob(jj.getJob());
		System.out.println("Jenkins build for "+br.getStoragefolder()+" launched on "+jj.getUrl()+" on job "+jj.getJob());
		return new Operation("Build Started").toEncryptedMessage(encryption).encodeBase64();
	}

	private EncryptedMessage processOperation(EncryptedMessage message) {
		Operation r = message.decodeBase64ToObject();
		if (r.getMessage().equals("STOP_DEQUEUE_BUILDS")) {
			MessageListenerContainer buiContainer = listenersreg.getListenerContainer("bui");
			if(buiContainer!=null) {
				buiContainer.stop();
				Operation op = new Operation("BUI_LISTENER_STOP");
				log.info("BUILDER DEQUEING STATUS " + (buiContainer.isRunning()?"RUNNING":"NOT RUNNING") );
				return op.toEncryptedMessage(encryption).encodeBase64();
			}
		} else if (r.getMessage().equals("START_DEQUEUE_BUILDS")){
			MessageListenerContainer buiContainer = listenersreg.getListenerContainer("bui");
			if(buiContainer!=null) {
				buiContainer.start();
				Operation op = new Operation("BUI_LISTENER_STARTED");
				log.info("BUILDER DEQUEING STATUS " + (buiContainer.isRunning()?"RUNNING":"NOT RUNNING") );
				return op.toEncryptedMessage(encryption).encodeBase64();
			}
		}else if (r.getMessage().equals("STOP_DEQUEUE_ANALYSIS")){
			System.out.println(r.getMessage());
			client.sendAndReceiveAna(r.toEncryptedMessage(encryption).encodeBase64());
		}else if (r.getMessage().equals("START_DEQUEUE_ANALYSIS")){
			System.out.println(r.getMessage());
			client.sendAndReceiveAna(r.toEncryptedMessage(encryption).encodeBase64());
		}else
			System.out.println(r.getMessage());
		
		return new Operation("NOP").toEncryptedMessage(encryption).encodeBase64();
	}
}
