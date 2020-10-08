import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Scanner;

public class AtomicMulticast {
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Uso: AtomicMulticast <grupo> <porta> <nickname>");
			return;
		}

		String mensagem = null;

		int porta = Integer.parseInt(args[1]);		
		MulticastSocket socket = new MulticastSocket(porta);
		InetAddress grupo = InetAddress.getByName(args[0]);
		socket.joinGroup(grupo);
		String nick = args[2];
		
		boolean voting = false;

		Scanner scanner = new Scanner(System.in);
		long last_time = 0;
		
		while (true) {
			try {
				byte[] entrada = new byte[1024];
				DatagramPacket pacote = new DatagramPacket(entrada,entrada.length);
				socket.setSoTimeout(100);
				socket.receive(pacote);
				String recebido = new String(pacote.getData(),0,pacote.getLength());
				
				String vars[] = recebido.split("\\s");

				try {

					// Verifica se a mensagem é antiga
					if (vars[0].equals(Long.toString(last_time))) {
						if (!vars[1].equals(nick)){
							System.out.println("old: " + recebido);
						}
					}
					// Caso a mensagem seja um VOTE_REQUEST, aguarda para o usuário fazer a votação. 
					// Flag voting para dizer que como recebeu um VOTE_REQUEST, a mesma esta fazendo a ação de votar. 
					else if (vars[2].equals("VOTE_REQUEST") && !vars[1].equals(nick)) {
						System.out.println("VOTE_REQUEST");
						voting = true;
					} 
					// Caso a mensagem seja um VOTE_ABORT, envia um GLOBAL_ABORT a todo grupo. Apenas um abort basta para tal ação.
					else if (vars[2].equals("VOTE_ABORT")) {
						mensagem = null;
						long time = System.currentTimeMillis();
						byte[] saida = new byte[1024];
						String mens = "GLOBAL_ABORT";
						saida = (Long.toString(time) + " " + nick + " " + mens).getBytes();
						DatagramPacket abort = new DatagramPacket(saida, saida.length, grupo, porta);
						socket.send(abort);

					}
					// Caso a mensagem seja um VOTE_ABORT, envia um GLOBAL_COMMIT a todo grupo. Apenas um commit basta para tal ação.
					// O grupo gostaria de fazer com que todo o multicast group tivesse que votar, porém não conseguimos determinar o tamanho
					// e assim, não sabemos quantos votos aguardar.
					else if (vars[2].equals("VOTE_COMMIT")) {
							if (mensagem != null) {
								String commit = "\tGLOBAL_COMMIT";
								mensagem += commit;
								byte[] saidaMensagem = mensagem.getBytes();
								DatagramPacket mensagemPack = new DatagramPacket(saidaMensagem, saidaMensagem.length, grupo, porta);
								socket.send(mensagemPack);
								mensagem = null;
							}
					}else {
						System.out.println("new: " + recebido);
						last_time = Long.parseLong(vars[0]);
						
						byte[] saida = new byte[1024];
						saida = recebido.getBytes();
						DatagramPacket pacote2 = new DatagramPacket(saida, saida.length, grupo, porta);
						socket.send(pacote2);

						
						if ("fim".equals(vars[2]))
							break;
					}
					
				} catch(ArrayIndexOutOfBoundsException e) {
				}
								
			} catch (IOException e){
			}
			
			if (System.in.available() > 0) {
				String mens = scanner.nextLine();
				long time = System.currentTimeMillis();
				byte[] saida = new byte[1024];
				String msgOut = (Long.toString(time) + " " + nick + " " + mens);
				saida = msgOut.getBytes();

				// Veririca se o usuario está votando e as opções digitadas não são as válidas.
				if (voting && (!mens.equals("VOTE_COMMIT") && !mens.equals("VOTE_ABORT"))) {
					System.out.println("OPCAO INVALIDA. Digite VOTE_COMMIT ou VOTE_ABORT");
					continue;
				}

				voting = false;

				// Verifica se o usuario nao esta votando e nao utilizou nenhuma das opcoes. 
				// Entao, o mesmo esta digitando a primeira mensagem, e assim a guardamos.
				if (!mens.equals("VOTE_COMMIT") && !mens.equals("VOTE_ABORT")) {
					mensagem = msgOut;
					byte[] msgVote = new byte[1024];
					msgVote = (Long.toString(time) + " " + nick + " " + "VOTE_REQUEST").getBytes();
					DatagramPacket votePacote = new DatagramPacket(msgVote, msgVote.length, grupo, porta);
					socket.send(votePacote);
				} else {
					DatagramPacket pacote = new DatagramPacket(saida, saida.length, grupo, porta);
					socket.send(pacote);
				}

			}
		}
		
		socket.leaveGroup(grupo);
		socket.close();
	}
}
