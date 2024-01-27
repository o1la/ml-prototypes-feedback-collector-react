package protopnet.mlprototypesfeedbackcollector.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator; 

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;

@Controller
public class ImageController {


    @Value("${local_analysis.static.path}")
    private String LOCAL_ANALYSIS_PATH;

    @Value("${modeldir.static.path}")
    private String MODELDIR_PATH;

    @Value("${model.static.path}")
    private String MODEL_PATH;

    @Value("${images_direct.static.path}")
    private String STATIC_IMAGES_PATH;

    @GetMapping("/picture-selection")
    public String showBirdSelection(Model model, HttpSession session) {
        File birdPictureFolder = new File(STATIC_IMAGES_PATH);
        Map<String, String> birdToFolder = new HashMap<>();

        if (birdPictureFolder.exists() && birdPictureFolder.isDirectory()) {
            String[] birdNames = birdPictureFolder.list();
            Arrays.sort(birdNames, Comparator.comparingInt(name -> Integer.parseInt(name.substring(0, 3))));

            //print out birdNames
            // for (String birdName : birdNames) {
            //     System.out.println(birdName);
            // }


            if (birdNames != null) {
                List<String> processedBirdNames = Arrays.stream(birdNames)
                        .map(name -> {
                            String processedName = name.substring(name.indexOf('.') + 1).replace('_', ' ');
                            birdToFolder.put(processedName, name);
                            return processedName;
                        })
                        .sorted()
                        .collect(Collectors.toList());
                        

                model.addAttribute("birdNames", processedBirdNames);
                model.addAttribute("nameToFolderMap", birdToFolder);

                //print out processedBirdNames
                // for (String processedBirdName : processedBirdNames) {
                //     System.out.println(processedBirdName);
                // }


                session.setAttribute("birdNames", processedBirdNames);
                session.setAttribute("folderNames", birdNames);
            }
        }
        return "PictureSelection";
    }

    private List<String> getBirdProcessedNames(String[] birdNames) {
        return Arrays.stream(birdNames)
                        .map(name -> {
                            String processedName = name.substring(name.indexOf('.') + 1).replace('_', ' ');
                            return processedName;
                        })
                        .collect(Collectors.toList());
    }


    //Connecting with model
    @PostMapping("/selected-pictures")
    public String analyzeSelectedPictures(@RequestParam String selectedImageUrl, @RequestParam String birdKind,
            Model model, HttpSession session) {
        String[] imageUrls = selectedImageUrl.split(";");

        //System.out.println("\n\n\n\nselectedImageUrl: " + selectedImageUrl + "\n\n");

    

        // TO DO: Analyze each image separately
        for (String imageUrl : imageUrls) {
            String[] parts = selectedImageUrl.split("/");
            String imgclassStr = parts[parts.length - 2].substring(0, 3);
            int originalImgclass = Integer.parseInt(imgclassStr);
            int imgclass = originalImgclass - 1;
            String imgdir = parts[parts.length - 2];
            String img = parts[parts.length - 1];

            // System.out.println("imgclassStr: " + imgclassStr + "\norignalImgclass: " + originalImgclass
            //         + "\nimgclass: " + imgclass + "\nimgdir: " + imgdir + "\nimg: " + img + "\n\n");
           
             String analysisCommand = "python " + LOCAL_ANALYSIS_PATH + " -modeldir " + MODELDIR_PATH +
                    " -model " + MODEL_PATH + " -imgdir " + STATIC_IMAGES_PATH
                    + imgdir + " -img " + img + " -imgclass " + imgclass;

            try {
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", analysisCommand);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    String[] resultsLines = output.toString().split("\\n");
                    String predictedClass = resultsLines[8].substring(resultsLines[8].indexOf(":") + 1).trim();

                    List<Map<String, String>> prototypes = new ArrayList<>();

                    String[] prototypeInfo = resultsLines[13].split(":");
                    Map<String, String> prototypeMap = new HashMap<>();
                    prototypeMap.put("index", prototypeInfo[1].trim());
                    prototypeMap.put("classIdentity", prototypeInfo[1].trim());
                    prototypes.add(prototypeMap);

                    // Uncomment to get output to the terminal
                    //System.out.println(output + "\n\n");
                    String[] birdNames = (String[])session.getAttribute("folderNames");

                    model.addAttribute("birdNames", getBirdProcessedNames(birdNames));
                    model.addAttribute("predictedClass", Integer.parseInt(prototypeInfo[1].trim()));
                    model.addAttribute("analysisResults", output.toString());
                    model.addAttribute("originalImgclass", originalImgclass);
                    model.addAttribute("imgdir", imgdir);
                    model.addAttribute("img", img);

                } else {
                    model.addAttribute("error", "Error executing the analysis command. Exit code: " + exitCode);
                    return "Results";
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                model.addAttribute("error", "Error executing the analysis command: " + e.getMessage());
                return "Results";
            }
        }
        return "Results";
    }

    
    //Fetching images

    @GetMapping("/images/{folderName}")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getBirdImages(@PathVariable String folderName) {
        

       
            List<Map<String, String>> imageDatas = new ArrayList<>();
            File birdFolder = new File(STATIC_IMAGES_PATH + folderName);

            if (birdFolder.exists() && birdFolder.isDirectory()) {
                File[] birdImages = birdFolder.listFiles();
                if (birdImages != null) {
                    for (File file : birdImages) {
                        if (file.isFile()) {
                            try {
                                byte[] imageData = Files.readAllBytes(file.toPath());
                                String base64Image = Base64.getEncoder().encodeToString(imageData);

                                
                                Map<String, String> imageDataMap = new HashMap<>();
                                imageDataMap.put("imageData", base64Image);
                                imageDataMap.put("folderPath", folderName+"/"+file.getName());

                                imageDatas.add(imageDataMap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return ResponseEntity.ok(imageDatas);
            }

            

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/results_images/{imgdir}/{index}/{type}")
    public ResponseEntity<byte[]> getGeneratedImage(
            @PathVariable String imgdir,
            @PathVariable String index,
            @PathVariable String type) {

        String filePath;

        if (type.equals("prototype")) {
            filePath = STATIC_IMAGES_PATH
                    + imgdir
                    + "/vgg19/001/100_0push0.7411.pth/most_activated_prototypes/prototype_activation_map_by_top-"
                    + index + "_prototype.png";
        } else {
            filePath = STATIC_IMAGES_PATH
                    +
                    imgdir + "/vgg19/001/100_0push0.7411.pth/most_activated_prototypes/top-" + index
                    + "_activated_prototype_self_act.png";
        }

        try {
            Path path = Paths.get(filePath);
            byte[] imageData = Files.readAllBytes(path);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
