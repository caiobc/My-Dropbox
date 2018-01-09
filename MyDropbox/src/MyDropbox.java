import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class MyDropbox {
    static ArrayList<String> ListaArquivosPresente = new ArrayList<>();
    static ArrayList<String> ListaArquivosPassado = new ArrayList<>();
    static ArrayList<String> ListaNuvemPassado = new ArrayList<>();
    static ArrayList<String> ListaNuvemPresente = new ArrayList<>();
    static String folderPath = "CAMINHO-DA-PASTA"; //ALTERAR AQUI
    static File folder = new File(folderPath);
	static String bucketName = "NOME-DO-SEU-BUCKET"; //ALTERAR AQUI
    static BasicAWSCredentials creds = new BasicAWSCredentials("CREDENCIAIS-DO-SEU-PERFIL", "CHAVE-DO-PERFIL"); //ALTERAR AQUI
	static AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion("sa-east-1") .build();
    
    public static void main(String[] args) throws InterruptedException, IOException {
    	initializeDirectory(); //Inicializa as Listas de arquivo e atualiza tanto a nuvem quanto o diretório
    	
        while(true){
            Thread.sleep(5000);
        	System.out.println("Analisando alterações no diretório");
            File[] listOfFiles = folder.listFiles();
            
            for (File listOfFile : listOfFiles) {
                ListaArquivosPresente.add(listOfFile.getName());
            }
            
            
        	compareListsDirectory(ListaArquivosPresente, ListaArquivosPassado);
        	ListaArquivosPassado.clear();
        	ListaArquivosPassado.addAll(ListaArquivosPresente);
        	ListaArquivosPresente.clear();
          
            
            
            Thread.sleep(5000);
            System.out.println("Analisando alterações na nuvem");
            try {
                final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withMaxKeys(2);
                ListObjectsV2Result result;
                do {               
                   result = s3Client.listObjectsV2(req);
                   
                   for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                	   ListaNuvemPresente.add(objectSummary.getKey());
                       //System.out.println(objectSummary.getKey());
                   }
                   req.setContinuationToken(result.getNextContinuationToken());
                } while(result.isTruncated() == true ); 
                
             } catch (AmazonServiceException ase) {
                System.out.println("Caught an AmazonServiceException, " +
                		"which means your request made it " +
                        "to Amazon S3, but was rejected with an error response " +
                        "for some reason.");
                System.out.println("Error Message:    " + ase.getMessage());
                System.out.println("HTTP Status Code: " + ase.getStatusCode());
                System.out.println("AWS Error Code:   " + ase.getErrorCode());
                System.out.println("Error Type:       " + ase.getErrorType());
                System.out.println("Request ID:       " + ase.getRequestId());
            } catch (AmazonClientException ace) {
                System.out.println("Caught an AmazonClientException, " +
                		"which means the client encountered " +
                        "an internal error while trying to communicate" +
                        " with S3, " +
                        "such as not being able to access the network.");
                System.out.println("Error Message: " + ace.getMessage());
            }
            
            
        	compareListsCloud(ListaNuvemPresente, ListaNuvemPassado);
        	ListaNuvemPassado.clear();
        	ListaNuvemPassado.addAll(ListaNuvemPresente);
        	ListaNuvemPresente.clear();
                    
        }
    }
    
    private static void initializeDirectory() throws IOException {
    	try {
    		System.out.println("Verificando novos arquivos");
            final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withMaxKeys(2);
            ListObjectsV2Result result;
            do {               
               result = s3Client.listObjectsV2(req);
               
               for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
            	   ListaNuvemPassado.add(objectSummary.getKey());
                   //System.out.println(objectSummary.getKey());
               }
               req.setContinuationToken(result.getNextContinuationToken());
            } while(result.isTruncated() == true );
            
         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, " +
            		"which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, " +
            		"which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    	
    	File[] listOfFiles = folder.listFiles();
        for (File listOfFile : listOfFiles) {
            ListaArquivosPassado.add(listOfFile.getName());
        }
    	
        for (int i = 0; i < ListaNuvemPassado.size(); i++) {
        	if(!ListaArquivosPassado.contains(ListaNuvemPassado.get(i))) {
        		System.out.println("O arquivo "+ListaNuvemPassado.get(i)+" foi detectado na nuvem");
        		downloadFileFromCloud(ListaNuvemPassado.get(i));
        	}
        }
        
        for (int i = 0; i < ListaArquivosPassado.size(); i++) {
        	if(!ListaNuvemPassado.contains(ListaArquivosPassado.get(i))) {
        		System.out.println("O arquivo "+ListaArquivosPassado.get(i)+" foi detectado no diretório");
        		uploadFileToCloud(ListaArquivosPassado.get(i));
        	}
        }
    }
    
    private static void compareListsDirectory(ArrayList<String> ListaArquivosPresente, ArrayList<String> ListaArquivosPassado) {
        for(int i = 0; i < ListaArquivosPresente.size(); i++){
            if(!ListaArquivosPassado.contains(ListaArquivosPresente.get(i)) && !s3Client.doesObjectExist(bucketName, ListaArquivosPresente.get(i))){
            	System.out.println("Está faltando o arquivo "+ ListaArquivosPresente.get(i) +" na nuvem");
                uploadFileToCloud(ListaArquivosPresente.get(i)); //Sobe para a Nuvem o arquivo inserido no Diretório.
            }
        }
        for(int i = 0 ; i < ListaArquivosPassado.size() ; i++ ){
            if(!ListaArquivosPresente.contains(ListaArquivosPassado.get(i)) && s3Client.doesObjectExist(bucketName, ListaArquivosPassado.get(i))){
            	System.out.println("O arquivo "+ ListaArquivosPassado.get(i) +" foi deletado do diretório");
                deleteFileInCloud(ListaArquivosPassado.get(i)); //Deleta da Nuvem o arquivo removido do Diretório.
            }
        }
    }
    
    private static void compareListsCloud(ArrayList<String> ListaNuvemPresente, ArrayList<String> ListaNuvemPassado) throws IOException{
    	for(int i = 0; i < ListaNuvemPresente.size(); i++){
    		if(!ListaNuvemPassado.contains(ListaNuvemPresente.get(i)) && !new File(folderPath+"/"+ListaNuvemPresente.get(i)).exists()) {
    			System.out.println("Está faltando o arquivo "+ ListaNuvemPresente.get(i) +" no diretório");
    			downloadFileFromCloud(ListaNuvemPresente.get(i)); //Baixa para o Diretório o arquivo inserido na Nuvem.
    		}
        }
    	
    	for(int i = 0; i < ListaNuvemPassado.size(); i++){
    		if(!ListaNuvemPresente.contains(ListaNuvemPassado.get(i)) && new File(folderPath+"/"+ListaNuvemPassado.get(i)).exists()) {
    			System.out.println("O arquivo "+ ListaNuvemPassado.get(i) +" foi deletado da nuvem");
    			deleteFileInDirectory(ListaNuvemPassado.get(i)); //Deleta do Diretório o arquivo removido da Nuvem.
    		}
    	}
    }
    
    private static void uploadFileToCloud(String fileName) {
    	String uploadFileName = folderPath+"/"+fileName; //folderPath = "/C:/Users/Caio/Desktop/MyDropbox" e fileName = "a.txt"
    	try {
    		
			System.out.println("Realizando upload do arquivo "+fileName);
			s3Client.putObject(new PutObjectRequest(bucketName, fileName, new File(uploadFileName)));
			System.out.println("Upload completo");

         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        }
    }
    
    private static void deleteFileInCloud(String fileName) {
    	System.out.println("Deletando o arquivo "+ fileName +" da nuvem");
    	s3Client.deleteObject(bucketName, fileName);
    	System.out.println("Delete completo");
    }
    
    private static void downloadFileFromCloud(String fileName) throws IOException {
    	File path = new File(folderPath+"/"+fileName);
    	try {
            System.out.println("Realizando download do arquivo "+fileName);
            S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, fileName));
            InputStream in = s3object.getObjectContent();
            Files.copy(in, path.toPath());
            System.out.println("Download completo");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which" +
            		" means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means"+
            		" the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
    
    private static void deleteFileInDirectory(String fileName){
		File filePath = new File(folderPath+"/"+fileName);
		
		try{
	    	System.out.println("Deletando o arquivo " + fileName + " do diretório");
			filePath.delete();
	    	System.out.println("Delete completo");
		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Something Went Wrong!", " ", JOptionPane.ERROR_MESSAGE);
		}
	}
}