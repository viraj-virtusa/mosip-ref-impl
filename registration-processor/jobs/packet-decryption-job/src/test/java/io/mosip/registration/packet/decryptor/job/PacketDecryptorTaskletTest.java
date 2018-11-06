package io.mosip.registration.packet.decryptor.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.test.context.junit4.SpringRunner;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.kernel.auditmanager.builder.AuditRequestBuilder;
import io.mosip.kernel.auditmanager.request.AuditRequestDto;
import io.mosip.registration.processor.core.builder.CoreAuditRequestBuilder;
import io.mosip.registration.processor.core.spi.filesystem.adapter.FileSystemAdapter;
import io.mosip.registration.processor.filesystem.ceph.adapter.impl.FilesystemCephAdapterImpl;
import io.mosip.registration.processor.packet.archiver.util.PacketArchiver;
import io.mosip.registration.processor.packet.archiver.util.exception.PacketNotFoundException;
import io.mosip.registration.processor.packet.archiver.util.exception.UnableToAccessPathException;
import io.mosip.registration.processor.packet.decryptor.job.Decryptor;
import io.mosip.registration.processor.packet.decryptor.job.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.packet.decryptor.job.exception.constant.PacketDecryptionFailureExceptionConstant;
import io.mosip.registration.processor.packet.decryptor.job.messagesender.DecryptionMessageSender;
import io.mosip.registration.processor.packet.decryptor.job.tasklet.PacketDecryptorTasklet;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class PacketDecryptorTaskletTest.
 * 
 */
@RefreshScope
@RunWith(SpringRunner.class)
public class PacketDecryptorTaskletTest {

	/** The Constant stream. */
	private static final InputStream stream = Mockito.mock(InputStream.class);

	/** The packet decryptor tasklet. */
	@InjectMocks
	PacketDecryptorTasklet packetDecryptorTasklet;

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The adapter. */
	@Mock
	FilesystemCephAdapterImpl adapter;

	@Mock
	private DecryptionMessageSender decryptionMessageSender;
	/** The decryptor. */
	@Mock
	private Decryptor decryptor;

	/** The packet archiver. */
	@Mock
	private PacketArchiver packetArchiver;

	/** The step contribution. */
	@MockBean
	StepContribution stepContribution;

	/** The chunk context. */
	@MockBean
	ChunkContext chunkContext;

	/** The dto. */
	InternalRegistrationStatusDto dto;

	/** The list. */
	List<InternalRegistrationStatusDto> list;

	@Mock
    private CoreAuditRequestBuilder coreAuditRequestBuilder = new CoreAuditRequestBuilder();
	/**
	 * Setup.
	 *
	 * @throws UnableToAccessPathException
	 *             the unable to access path exception
	 * @throws PacketNotFoundException
	 *             the packet not found exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Before
	public void setup() throws UnableToAccessPathException, PacketNotFoundException, IOException {
		dto = new InternalRegistrationStatusDto();
		dto.setRegistrationId("1001");
		dto.setStatusCode("PACKET_UPLOADED_TO_FILESYSTEM");
		dto.setRetryCount(0);
		list = new ArrayList<InternalRegistrationStatusDto>();
        AuditRequestBuilder auditRequestBuilder = new AuditRequestBuilder();
        AuditRequestDto auditRequest1 = new AuditRequestDto();

        Field f = CoreAuditRequestBuilder.class.getDeclaredField("auditRequestBuilder");
        f.setAccessible(true);
        f.set(coreAuditRequestBuilder, auditRequestBuilder);
        Field f1 = AuditRequestBuilder.class.getDeclaredField("auditRequest");
        f1.setAccessible(true);
        f1.set(auditRequestBuilder, auditRequest1);
	}

	/**
	 * Decryption success test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void decryptionSuccessTest() throws Exception {

		list.add(dto);
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		UnableToAccessPathException exception = new UnableToAccessPathException("", "Unable to access path Exception");
		Mockito.doThrow(exception).when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.when(
				registrationStatusService.getByStatus(RegistrationStatusCode.PACKET_UPLOADED_TO_FILESYSTEM.toString()))
				.thenReturn(list);

		Mockito.doNothing().when(adapter).unpackPacket(any(String.class));

		Mockito.when(adapter.getPacket(any(String.class))).thenReturn(stream);

		Mockito.when(decryptor.decrypt(any(InputStream.class), any(String.class))).thenReturn(stream);

		Mockito.doNothing().when(registrationStatusService)
				.updateRegistrationStatus(any(InternalRegistrationStatusDto.class));

		Mockito.doNothing().when(decryptionMessageSender).sendMessage(any(MessageDTO.class));
		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains("Packet decrypted and extracted encrypted files stored in DFS.");
			}
		}));
	}

	/**
	 * Null packet test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void nullPacketTest() throws Exception {

		list.add(dto);
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		PacketNotFoundException exception = new PacketNotFoundException("", "Packet not found Exception");
		Mockito.doThrow(exception).when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.when(
				registrationStatusService.getByStatus(RegistrationStatusCode.PACKET_UPLOADED_TO_FILESYSTEM.toString()))
				.thenReturn(list);

		Mockito.doNothing().when(adapter).unpackPacket(any(String.class));

		Mockito.when(adapter.getPacket(any(String.class))).thenReturn(stream);

		Mockito.when(decryptor.decrypt(any(InputStream.class), any(String.class))).thenReturn(null);

		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains(" Packet is null and could not be  decrypted ");
			}
		}));
	}

	/**
	 * Decryption failure test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void decryptionFailureTest() throws Exception {

		list.add(dto);
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		Mockito.doNothing().when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.when(
				registrationStatusService.getByStatus(RegistrationStatusCode.PACKET_UPLOADED_TO_FILESYSTEM.toString()))
				.thenReturn(list);

		Mockito.doNothing().when(adapter).unpackPacket(any(String.class));

		Mockito.when(adapter.getPacket(any(String.class))).thenReturn(stream);

		PacketDecryptionFailureException exception = new PacketDecryptionFailureException(
				PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
				PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE.getErrorMessage(),
				new IOException());

		Mockito.when(decryptor.decrypt(any(InputStream.class), any(String.class))).thenThrow(exception);

		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains(PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE
								.getErrorMessage());
			}
		}));
	}

	/**
	 * No files to be decrypted test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void noFilesToBeDecryptedTest() throws Exception {

		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		Mockito.doNothing().when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.when(
				registrationStatusService.getByStatus(RegistrationStatusCode.PACKET_UPLOADED_TO_FILESYSTEM.toString()))
				.thenReturn(list);

		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains("There are currently no files to be decrypted");
			}
		}));
	}

	/**
	 * Status update exception test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void StatusUpdateExceptionTest() throws Exception {
		list.add(dto);

		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		Mockito.doNothing().when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.when(
				registrationStatusService.getByStatus(RegistrationStatusCode.PACKET_UPLOADED_TO_FILESYSTEM.toString()))
				.thenReturn(list);

		Mockito.doNothing().when(adapter).unpackPacket(any(String.class));

		Mockito.when(adapter.getPacket(any(String.class))).thenReturn(stream);

		Mockito.when(decryptor.decrypt(any(InputStream.class), any(String.class))).thenReturn(stream);

		Mockito.doThrow(TablenotAccessibleException.class).when(registrationStatusService)
				.updateRegistrationStatus(any(InternalRegistrationStatusDto.class));

		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains("The Registration Status table is not accessible");
			}
		}));
	}

	/**
	 * Gets the by satus exception test.
	 *
	 * @return the by satus exception test
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void getBySatusExceptionTest() throws Exception {

		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);
		Mockito.doNothing().when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.doThrow(TablenotAccessibleException.class).when(registrationStatusService)
				.getByStatus(any(String.class));

		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains("The Registration Status table is not accessible");
			}
		}));
	}

	/**
	 * IO exception test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void IOExceptionTest() throws Exception {

		byte[] by = new byte[2];
		by[0] = 1;
		by[1] = 2;
		InputStream stream = new ByteArrayInputStream(by);
		list.add(dto);
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		Mockito.doNothing().when(packetArchiver).archivePacket(ArgumentMatchers.any());

		Mockito.when(
				registrationStatusService.getByStatus(RegistrationStatusCode.PACKET_UPLOADED_TO_FILESYSTEM.toString()))
				.thenReturn(list);

		Mockito.when(adapter.getPacket(any(String.class))).thenReturn(stream);
		Mockito.when(decryptor.decrypt(any(InputStream.class), any(String.class))).thenReturn(stream);
		Mockito.doThrow(IOException.class).when(adapter).unpackPacket(any(String.class));

		RepeatStatus status = packetDecryptorTasklet.execute(stepContribution, chunkContext);
		Assert.assertEquals(RepeatStatus.FINISHED, status);
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
			@Override
			public boolean matches(final ILoggingEvent argument) {
				return ((ILoggingEvent) argument).getFormattedMessage()
						.contains("The DFS Path set by the System is not accessible");
			}
		}));
	}

}
