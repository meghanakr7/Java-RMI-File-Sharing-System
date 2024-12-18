import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteFileService extends Remote {
    String uploadFile(String clientFilePath, String serverFilePath, byte[] data) throws RemoteException;
    byte[] downloadFile(String serverFilePath) throws RemoteException;
    String listDirectory(String serverDirectoryPath) throws RemoteException;
    String createDirectory(String directoryPath) throws RemoteException;
    String removeDirectory(String directoryPath) throws RemoteException;
    String removeFile(String filePath) throws RemoteException;
    String shutdownServer() throws RemoteException;
    long getUploadedSize(String serverFilePath) throws RemoteException;
    String uploadChunk(String serverFilePath, byte[] data, int bytesRead) throws RemoteException;
    String setFileSize(String serverFilePath, long fileSize) throws RemoteException;
    long getFileSize(String serverFilePath) throws RemoteException;
    byte[] downloadChunk(String serverFilePath, long offset) throws RemoteException;


}
