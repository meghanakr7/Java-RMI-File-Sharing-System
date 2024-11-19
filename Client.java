import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.nio.file.Files;
import java.io.File;

public class Client {
    public static void main(String[] args) {
        String serverEnv = System.getenv("PA1_SERVER");
        if (serverEnv == null || !serverEnv.contains(":")) {
            System.out.println("PA1_SERVER environment variable is not set correctly.");
            return;
        }

        String[] serverInfo = serverEnv.split(":");
        String host = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            RemoteFileService stub = (RemoteFileService) registry.lookup("FileService");

            if (args.length == 0) {
                System.out.println("Usage: java -cp pa1.jar client <command> <args>");
                return;
            }

            switch (args[0].toLowerCase()) {
                case "upload":
                    byte[] data = Files.readAllBytes(new File(args[1]).toPath());
                    System.out.println(stub.uploadFile(args[1], args[2], data));
                    break;
                case "download":
                    byte[] fileData = stub.downloadFile(args[1]);
                    Files.write(new File(args[2]).toPath(), fileData);
                    System.out.println("File downloaded successfully.");
                    break;
                case "dir":
                    System.out.println(stub.listDirectory(args[1]));
                    break;
                case "mkdir":
                    System.out.println(stub.createDirectory(args[1]));
                    break;
                case "rmdir":
                    System.out.println(stub.removeDirectory(args[1]));
                    break;
                case "rm":
                    System.out.println(stub.removeFile(args[1]));
                    break;
                case "shutdown":
                    System.out.println(stub.shutdownServer());
                    break;
                default:
                    System.out.println("Invalid command.");
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
    