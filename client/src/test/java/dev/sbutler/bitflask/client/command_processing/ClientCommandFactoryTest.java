package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplString;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.network.RespServiceProvider;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ClientCommandFactory}. */
public class ClientCommandFactoryTest {

  private final OutputWriter outputWriter = mock(OutputWriter.class);
  private final RespCommandProcessor respCommandProcessor = mock(RespCommandProcessor.class);
  private final RespServiceProvider respServiceProvider = mock(RespServiceProvider.class);

  private final ClientCommandFactory clientCommandFactory =
      new ClientCommandFactory(outputWriter, respCommandProcessor, respServiceProvider);

  @Test
  public void createCommand_remoteCommand_ping() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("ping"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);

    assertThat(command).isInstanceOf(RemoteCommand.class);
    assertThat(((RemoteCommand) command).getRespRequest())
        .isInstanceOf(RespRequest.PingRequest.class);
  }

  @Test
  public void createCommand_remoteCommand_get() {
    ImmutableList<ReplElement> clientInput =
        ImmutableList.of(new ReplString("get"), new ReplString("key"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);

    assertThat(command).isInstanceOf(RemoteCommand.class);
    assertThat(((RemoteCommand) command).getRespRequest())
        .isInstanceOf(RespRequest.GetRequest.class);
  }

  @Test
  public void createCommand_remoteCommand_set() {
    ImmutableList<ReplElement> clientInput =
        ImmutableList.of(new ReplString("set"), new ReplString("key"), new ReplString("value"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);

    assertThat(command).isInstanceOf(RemoteCommand.class);
    assertThat(((RemoteCommand) command).getRespRequest())
        .isInstanceOf(RespRequest.SetRequest.class);
  }

  @Test
  public void createCommand_remoteCommand_delete() {
    ImmutableList<ReplElement> clientInput =
        ImmutableList.of(new ReplString("delete"), new ReplString("key"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);

    assertThat(command).isInstanceOf(RemoteCommand.class);
    assertThat(((RemoteCommand) command).getRespRequest())
        .isInstanceOf(RespRequest.DeleteRequest.class);
  }

  @Test
  public void createCommand_localCommand_help() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("help"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);

    assertThat(command).isInstanceOf(LocalCommand.Help.class);
  }

  @Test
  public void createCommand_localCommand_exit() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("exit"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);

    assertThat(command).isInstanceOf(LocalCommand.Exit.class);
  }

  @Test
  public void createCommand_localCommand_invalid() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("unknown"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);
    command.execute();

    assertThat(command).isInstanceOf(LocalCommand.Invalid.class);
    verify(outputWriter, atMostOnce()).writeWithNewLine("Unknown command [unknown].");
  }

  @Test
  public void createClientCommand_empty_invalid() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of();

    ClientCommand command = clientCommandFactory.createCommand(clientInput);
    command.execute();

    assertThat(command).isInstanceOf(LocalCommand.Invalid.class);
    verify(outputWriter, atMostOnce()).writeWithNewLine("");
  }

  @Test
  public void createCommand_remoteCommand_get_invalidArgs_invalid() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("get"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);
    command.execute();

    assertThat(command).isInstanceOf(LocalCommand.Invalid.class);
    verify(outputWriter, atMostOnce()).writeWithNewLine("The Get command requires a key.");
  }

  @Test
  public void createCommand_remoteCommand_set_invalidArgs_invalid() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("set"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);
    command.execute();

    assertThat(command).isInstanceOf(LocalCommand.Invalid.class);
    verify(outputWriter, atMostOnce())
        .writeWithNewLine("The Set command requires a key and value.");
  }

  @Test
  public void createCommand_remoteCommand_del_invalidArgs_invalid() {
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("delete"));

    ClientCommand command = clientCommandFactory.createCommand(clientInput);
    command.execute();

    assertThat(command).isInstanceOf(LocalCommand.Invalid.class);
    verify(outputWriter, atMostOnce()).writeWithNewLine("The Delete command requires a key.");
  }
}
