package com.training.messaging;

/**
 * Stub — represents the separate socket server Jhefrey described (built
 * before he joined the project; he only ever sent messages to it, never
 * implemented it). Not part of the exercise; do not modify.
 */
public interface SocketNotifier {
    void notifySuccess(String documentId);
    void notifyFailure(String documentId, String reason);
}
