import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.nio.file.Files;

public class Server implements RemoteFileService {
    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("start")) {
            System.out.println("Usage: java -cp pa1.jar server start <portnumber>");
            return;
        }

        try {
            int port = Integer.parseInt(args[1]);
            Server server = new Server();
            RemoteFileService stub = (RemoteFileService) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("FileService", stub);
            System.out.println("Server started on port " + port);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String uploadFile(String clientFilePath, String serverFilePath, byte[] data) throws RemoteException {
    try {
        // Ensure the server path is relative to the server's working directory
        File file = new File("server" + File.separator + serverFilePath); 

        // Create parent directories if they do not exist
        file.getParentFile().mkdirs();

        // Write the data to the file
        Files.write(file.toPath(), data);

        return "File uploaded successfully.";
    } catch (Exception e) {
        return "Error during upload: " + e.getMessage();
    }
    }


    @Override
    public byte[] downloadFile(String serverFilePath) throws RemoteException {
        try {
            File file = new File(serverFilePath);
            if (file.exists()) {
                return Files.readAllBytes(file.toPath());
            } else {
                throw new Exception("File not found.");
            }
        } catch (Exception e) {
            throw new RemoteException("Error during download: " + e.getMessage());
        }
    }

    @Override
    public String listDirectory(String serverDirectoryPath) throws RemoteException {
        File dir = new File(serverDirectoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return "Directory not found.";
        }
        StringBuilder tree = new StringBuilder();
        for (File file : dir.listFiles()) {
            tree.append(file.getName()).append(file.isDirectory() ? "/" : "").append("\n");
        }
        return tree.toString();
    }

    @Override
    public String createDirectory(String directoryPath) throws RemoteException {
        File dir = new File(directoryPath);
        if (dir.mkdir()) {
            return "Directory created successfully.";
        } else {
            return "Error creating directory.";
        }
    }

    @Override
    public String removeDirectory(String directoryPath) throws RemoteException {
        File dir = new File(directoryPath);
        if (dir.delete()) {
            return "Directory removed successfully.";
        } else {
            return "Error removing directory.";
        }
    }

    @Override
    public String removeFile(String filePath) throws RemoteException {
        File file = new File(filePath);
        if (file.delete()) {
            return "File removed successfully.";
        } else {
            return "Error removing file.";
        }
    }

    @Override
    public String shutdownServer() throws RemoteException {
        System.exit(0);
        return "Server shutting down.";
    }
}
