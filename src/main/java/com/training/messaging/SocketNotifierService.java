package com.training.messaging;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SocketNotifierService implements SocketNotifier {

    private static final Logger logger = LoggerFactory.getLogger(SocketNotifierService.class);


    @Override
    public void notifySuccess(String documentId) {
        logger.debug("(stub) notifySuccess called for document {}", documentId);
    }

    @Override
    public void notifyFailure(String documentId, String reason) {
        logger.debug("(stub) notifyFailure called for document {} — reason: {}", documentId, reason);
    }
}
