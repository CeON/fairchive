package edu.harvard.iq.dataverse.error;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link FallbackExceptionHandler}.
 *
 * @author Arkadiusz Kowal
 */
public class FallbackExceptionHandlerTest {

    private static final String SEND_ERROR = "SEND_ERROR";
    private static final String ADD_MESSAGE = "ADD_MESSAGE";
    private static final String LOG_PREFIX = "LOG:";

    private static final Logger handlerLogger =
            Logger.getLogger(FallbackExceptionHandler.class.getName());

    private final List<String> callOrder = new ArrayList<>();
    private final List<LogRecord> logRecords = new ArrayList<>();
    private final RecordingLogHandler logHandler = new RecordingLogHandler();

    private final List<ExceptionQueuedEvent> queuedEvents = new ArrayList<>();

    private ExceptionHandler wrapped;
    private FacesContext facesContext;
    private ExternalContext externalContext;
    private PartialViewContext partialViewContext;

    private FallbackExceptionHandler handler;

    @BeforeEach
    public void setUp() throws Exception {
        handlerLogger.addHandler(logHandler);

        wrapped = mock(ExceptionHandler.class);
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(queuedEvents);

        facesContext = FacesContextMocker.mockContext();
        externalContext = facesContext.getExternalContext();
        partialViewContext = mock(PartialViewContext.class);

        when(facesContext.getPartialViewContext()).thenReturn(partialViewContext);
        when(facesContext.getAttributes()).thenReturn(new HashMap<>());
        when(externalContext.getRequestLocale()).thenReturn(Locale.ENGLISH);

        doAnswer(invocation -> callOrder.add(SEND_ERROR))
                .when(externalContext).responseSendError(anyInt(), any());
        doAnswer(invocation -> callOrder.add(ADD_MESSAGE))
                .when(facesContext).addMessage(isNull(), any(FacesMessage.class));

        handler = new FallbackExceptionHandler(wrapped);
    }

    @AfterEach
    public void tearDown() {
        handlerLogger.removeHandler(logHandler);
        facesContext.release();
    }

    // -------------------- TESTS --------------------

    @Test
    public void handle__logsAndDrainsWholeQueueBeforeRespondingToTheUser() {
        // given
        nonAjaxRequest();
        Throwable first = new IllegalStateException("first");
        Throwable second = new IllegalArgumentException("second");
        Throwable third = new UnsupportedOperationException("third");
        queue(first, second, third);

        // when
        handler.handle();

        // then
        assertThat(logRecords).extracting(LogRecord::getThrown)
                .containsExactly(first, second, third);
        assertThat(queuedEvents).isEmpty();
        assertThat(callOrder).containsExactly(
                logged(IllegalStateException.class),
                logged(IllegalArgumentException.class),
                logged(UnsupportedOperationException.class),
                SEND_ERROR);
    }

    @Test
    public void handle__doesNothingWhenNoExceptionIsQueued() throws Exception {
        // given
        // nothing is queued

        // when
        handler.handle();

        // then
        assertThat(logRecords).isEmpty();
        verify(externalContext, never()).responseSendError(anyInt(), any());
        verify(facesContext, never()).addMessage(isNull(), any(FacesMessage.class));
    }

    @Test
    public void handle__sendsError500ForNonAjaxRequest() throws Exception {
        // given
        nonAjaxRequest();
        queue(new IllegalStateException("boom"));

        // when
        handler.handle();

        // then
        verify(externalContext).responseSendError(500, null);
        verify(facesContext, never()).addMessage(isNull(), any(FacesMessage.class));
    }

    @Test
    public void handle__addsErrorMessageForAjaxRequest() throws Exception {
        // given
        ajaxRequest();
        queue(new IllegalStateException("boom"));

        // when
        handler.handle();

        // then
        verify(facesContext).addMessage(isNull(), any(FacesMessage.class));
        verify(externalContext, never()).responseSendError(anyInt(), any());
    }

    @Test
    public void handle__logsConstantMessageInsteadOfTheExceptionMessage() {
        // given
        // A message-less exception used to produce a record with no message at
        // all, which reads as an empty line in server.log.
        nonAjaxRequest();
        Throwable messageless = new IllegalStateException();
        queue(messageless);

        // when
        handler.handle();

        // then
        assertThat(logRecords).hasSize(1);
        assertThat(logRecords.get(0).getMessage())
                .isEqualTo("Unhandled exception while serving a JSF request");
        assertThat(logRecords.get(0).getThrown()).isSameAs(messageless);
    }

    @Test
    public void handle__logsWhatFailedWhenTheErrorPageCannotBeSent() throws Exception {
        // given
        // responseSendError throws IllegalStateException once the response is
        // committed, and that exception carries no message of its own.
        nonAjaxRequest();
        IllegalStateException committed = new IllegalStateException();
        doAnswer(invocation -> {
            throw committed;
        }).when(externalContext).responseSendError(anyInt(), any());
        queue(new IllegalArgumentException("original"));

        // when
        handler.handle();

        // then
        assertThat(logRecords).hasSize(2);
        assertThat(logRecords.get(1).getThrown()).isSameAs(committed);
        assertThat(logRecords.get(1).getMessage())
                .isEqualTo("Failed to send the HTTP 500 error page");
    }

    @Test
    public void handle__logsWholeQueueWhenThereIsNoFacesContextLeftToRespondWith() {
        // given
        // Responding is what can blow up - here the context is already gone, so
        // there is nothing left to answer with. Draining first keeps the log
        // complete regardless, and the last line of defence must not throw.
        facesContext.release();
        Throwable first = new IllegalStateException("first");
        Throwable second = new IllegalArgumentException("second");
        queue(first, second);

        // when
        handler.handle();

        // then
        assertThat(logRecords).extracting(LogRecord::getThrown).containsExactly(first, second);
        assertThat(queuedEvents).isEmpty();
        assertThat(callOrder).containsExactly(
                logged(IllegalStateException.class),
                logged(IllegalArgumentException.class));
    }

    // -------------------- PRIVATE --------------------

    private void queue(Throwable... exceptions) {
        for (Throwable exception : exceptions) {
            queuedEvents.add(new ExceptionQueuedEvent(
                    new ExceptionQueuedEventContext(facesContext, exception)));
        }
    }

    private static String logged(Class<? extends Throwable> exceptionType) {
        return LOG_PREFIX + exceptionType.getName();
    }

    private void ajaxRequest() {
        when(partialViewContext.isAjaxRequest()).thenReturn(true);
    }

    private void nonAjaxRequest() {
        when(partialViewContext.isAjaxRequest()).thenReturn(false);
    }

    // -------------------- INNER CLASSES --------------------

    private class RecordingLogHandler extends Handler {

        // -------------------- LOGIC --------------------

        @Override
        public void publish(LogRecord record) {
            logRecords.add(record);
            Throwable thrown = record.getThrown();
            callOrder.add(thrown != null ? logged(thrown.getClass()) : LOG_PREFIX + record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
