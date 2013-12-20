package org.hive2hive.core.test.network.messages;

import org.hive2hive.core.network.messages.AcceptanceReply;
import org.hive2hive.core.network.messages.BaseMessage;

public class TestSignedMessage extends BaseMessage {

	private static final long serialVersionUID = -7416023464387691292L;

	public TestSignedMessage(String targetKey) {
		super(createMessageID(), targetKey);
	}

	@Override
	public void run() {
	}

	@Override
	public AcceptanceReply accept() {
		return AcceptanceReply.OK;
	}

	@Override
	public boolean checkSignature(byte[] data, byte[] signature, String userId) {
		if (!networkManager.getUserId().equals(userId)) {
			return false;
		} else {
			return verify(data, signature, networkManager.getPublicKey());
		}
	}

}