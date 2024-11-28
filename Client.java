import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.nio.file.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
                    File file = new File(args[1]);
                    if (!file.exists()) {
                        System.out.println("Error: File not found.");
                        break;
                    }

                    long fileSize = file.length();
                    long alreadyUploaded = 0;

                    // Connect to the server
                    registry = LocateRegistry.getRegistry(System.getenv("PA1_SERVER").split(":")[0],
                            Integer.parseInt(System.getenv("PA1_SERVER").split(":")[1]));
                    stub = (RemoteFileService) registry.lookup("FileService");

                    // Get the already uploaded size from the server
                    alreadyUploaded = stub.getUploadedSize(args[2]);
                    System.out.println("Resuming upload from " + alreadyUploaded + " bytes.");

                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        fileInputStream.skip(alreadyUploaded);
                        byte[] buffer = new byte[4096]; // 4 KB buffer
                        int bytesRead;
                        long totalUploaded = alreadyUploaded;

                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            // Send a chunk of data to the server
                            stub.uploadChunk(args[2], buffer, bytesRead);
                            totalUploaded += bytesRead;

                            // Display progress
                            int progress = (int) ((totalUploaded * 100) / fileSize);
                            displayProgress(totalUploaded, fileSize, progress);

                            // Slow down the upload process
                            Thread.sleep(500); // Simulate slower upload
                        }

                        System.out.println("\nFile uploaded successfully.");
                    } catch (Exception e) {
                        System.out.println("Error during upload: " + e.getMessage());
                    }
                    break;


                    case "download":
                    try {
                        // Request the file data from the server
                        System.out.println("Starting download...");
                        fileSize = stub.getFileSize(args[1]); // Get the total file size from the server
                        System.out.println("Total file size: " + fileSize + " bytes.");
                
                        byte[] buffer;
                        long totalDownloaded = 0;
                
                        // Loop to receive chunks
                        try (FileOutputStream fos = new FileOutputStream(new File(args[2]))) {
                            while ((buffer = stub.downloadChunk(args[1], totalDownloaded)) != null) {
                                fos.write(buffer);
                                totalDownloaded += buffer.length;
                
                                // Display progress
                                int progress = (int) ((totalDownloaded * 100) / fileSize);
                                displayProgress(totalDownloaded, fileSize, progress);
                
                                // Slow down the download process
                                Thread.sleep(5000); // Simulate slower download
                            }
                        }
                
                        System.out.println("\nFile downloaded successfully.");
                    } catch (Exception e) {
                        System.out.println("Error during download: " + e.getMessage());
                    }
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
    public static void uploadFile(String clientFilePath, String serverFilePath) throws IOException {
    try {
        File file = new File(clientFilePath);
        if (!file.exists()) {
            System.err.println("Error: File '" + clientFilePath + "' does not exist.");
            return;
        }

        long fileSize = file.length();
        long alreadyUploaded = 0;

        // Connect to the server
        Registry registry = LocateRegistry.getRegistry(System.getenv("PA1_SERVER").split(":")[0],
                Integer.parseInt(System.getenv("PA1_SERVER").split(":")[1]));
        RemoteFileService stub = (RemoteFileService) registry.lookup("FileService");

        // Get the already uploaded size from the server
        alreadyUploaded = stub.getUploadedSize(serverFilePath);
        System.out.println("Resuming upload from " + alreadyUploaded + " bytes.");

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.skip(alreadyUploaded);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalUploaded = alreadyUploaded;

            // Start the upload
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                stub.uploadChunk(serverFilePath, buffer, bytesRead);
                totalUploaded += bytesRead;

                // Display progress
                int progress = (int) ((totalUploaded * 100) / fileSize);
                displayProgress(totalUploaded, fileSize, progress);

                // Slow down the upload process
                Thread.sleep(5000); // Simulate delay for visibility
            }
            System.out.println("\nFile upload complete. Total bytes uploaded: " + totalUploaded);
        }
    } catch (Exception e) {
        System.err.println("Error during upload: " + e.getMessage());
    }
}

private static void displayProgress(long uploadedBytes, long totalBytes, int progress) {
    StringBuilder progressBar = new StringBuilder("[");
    int barLength = 50;
    for (int i = 0; i < barLength; i++) {
        if (i < progress / 2) {
            progressBar.append("=");
        } else {
            progressBar.append(" ");
        }
    }
    progressBar.append("] ").append(progress).append("% (")
               .append(uploadedBytes).append("/").append(totalBytes).append(" bytes)");

    System.out.print("\r" + progressBar.toString());
}

    
    

}
