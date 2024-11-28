import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements RemoteFileService {
private static Map<String, Long> uploadedSizes = new ConcurrentHashMap<>();
private static Map<String, Long> fileSizes = new ConcurrentHashMap<>();
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
        System.out.println("Func: UploadFile in server");
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
        listFilesRecursively(dir, tree, "");
        System.out.println("Iam in list dir");
        return tree.toString();
    }
    
    private void listFilesRecursively(File dir, StringBuilder tree, String indent) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                tree.append(indent).append(file.getName()).append("/").append("\n");
                listFilesRecursively(file, tree, indent + "    "); // Indentation for nested directories
            } else {
                tree.append(indent).append(file.getName()).append("\n");
            }
        }
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
        // Respond to the client before shutting down
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Allow the response to be sent before shutdown
                System.exit(0);    // Shut down the server
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    
        return "Server is shutting down. Goodbye!";
    }
    
   

    @Override
    public long getUploadedSize(String serverFilePath) throws RemoteException {
        System.out.println("Func: getUploadedSize" + uploadedSizes.getOrDefault(serverFilePath, 0L));
        return uploadedSizes.getOrDefault(serverFilePath, 0L);
    }
    
@Override
public String uploadChunk(String serverFilePath, byte[] data, int bytesRead) throws RemoteException {
    try {
        String currentDir = System.getProperty("user.dir");
        String fullPath = currentDir + File.separator + serverFilePath;
        System.out.println("Serverfile pah is  "+fullPath);
        File file = new File(fullPath);
        file.getParentFile().mkdirs();

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long uploadedSize = getUploadedSize(serverFilePath);
            raf.seek(uploadedSize);
            raf.write(data, 0, bytesRead);

            uploadedSizes.put(serverFilePath, uploadedSize + bytesRead);

            // Ensure total file size is known
            long fileSize = fileSizes.getOrDefault(serverFilePath, uploadedSizes.get(serverFilePath));
            fileSizes.put(serverFilePath, fileSize);

            // Debug statement
            System.out.println("Func: uploadChunk, Bytes Written: " + bytesRead + ", Total Uploaded: " + uploadedSizes.get(serverFilePath));
        }

        Thread.sleep(5000); // Simulate slower server processing

        // Display progress
        long totalUploaded = uploadedSizes.get(serverFilePath);
        long fileSize = fileSizes.get(serverFilePath); // Ensure correct total size
        int progress = (int) ((totalUploaded * 100) / fileSize);
        displayServerProgress(serverFilePath, totalUploaded, fileSize, progress);

        return "Chunk uploaded successfully.";
    } catch (Exception e) {
        return "Error during chunk upload: " + e.getMessage();
    }
}

        @Override
        public String setFileSize(String serverFilePath, long fileSize) throws RemoteException {
            fileSizes.put(serverFilePath, fileSize);
            return "File size set successfully.";
        }
// Display a progress bar on the server side
private static void displayServerProgress(String filePath, long uploadedBytes, long totalBytes, int progress) {
    System.out.println("Func: DisplayServerProgress");
    System.out.print("\rUploading " + filePath + ": [");
    int barLength = 50;
    for (int i = 0; i < barLength; i++) {
        if (i < progress / 2) {
            System.out.print("=");
        } else {
            System.out.print(" ");
        }
    }
    System.out.print("] " + progress + "% (" + uploadedBytes + "/" + totalBytes + " bytes)");
}
@Override
public long getFileSize(String serverFilePath) throws RemoteException {
    // Use the server's current working directory dynamically
    File file = new File(System.getProperty("user.dir") + File.separator + serverFilePath);
    if (!file.exists()) {
        throw new RemoteException("File not found in the server folder: " + file.getAbsolutePath());
    }

    // Debug output for server
    System.out.println("File size for " + file.getName() + ": " + file.length() + " bytes.");
    return file.length();
}

@Override
public byte[] downloadChunk(String serverFilePath, long offset) throws RemoteException {
    try {
        // Use the server's current working directory dynamically
        File file = new File(System.getProperty("user.dir") + File.separator + serverFilePath);
        if (!file.exists()) {
            throw new RemoteException("File not found in the server folder: " + file.getAbsolutePath());
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset); // Move to the specified offset
            byte[] buffer = new byte[4096]; // 4 KB buffer
            int bytesRead = raf.read(buffer);

            if (bytesRead == -1) {
                return null; // End of file
            }

            // Send only the portion of the buffer that was read
            byte[] actualData = new byte[bytesRead];
            System.arraycopy(buffer, 0, actualData, 0, bytesRead);

            // Debug output for server
            System.out.println("Sending " + bytesRead + " bytes from offset " + offset + " for file: " + file.getName());
            return actualData;
        }
    } catch (Exception e) {
        throw new RemoteException("Error during download: " + e.getMessage());
    }
}
}