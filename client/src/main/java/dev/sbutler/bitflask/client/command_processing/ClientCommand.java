package dev.sbutler.bitflask.client.command_processing;

public sealed interface ClientCommand permits LocalCommand, RemoteCommand {

}
