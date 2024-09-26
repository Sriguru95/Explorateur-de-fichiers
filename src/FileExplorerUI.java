import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileExplorerUI implements Runnable {

    private JFrame frame;

    private JLabel navigationLabel;
    private ArrayList<JList<String>> fileLists = new ArrayList<>();
    private JList<String> favoritesList;
    private JScrollPane columnsScrollPane;

    private JPanel fileContentViewerPanel;
    private JLabel fileNameLabel;
    private JLabel fileSizeLabel;
    private JLabel fileDateLabel;

    private Map<String, String> viewers;
    
    private JPanel searchPanel;
    private JLabel searchTitleLabel;
    private JList<String> searchResultsList;
    private List<String> filePaths = new ArrayList<>();


    public FileExplorerUI(JFrame frame) {
        this.frame = frame;
    }
    
    // M�thode pour v�rifier l'existence du fichier de configuration et le cr�er si n�cessaire
    private void createConfigFileIfNotExists() {
        File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
        if (!configFile.exists()) {
            // R�cup�rer le r�pertoire parent du fichier explorer.conf
            String parentDirectory = configFile.getParent();

            // Afficher tous les dossiers dans le r�pertoire parent
            File parentDir = new File(parentDirectory);
            File[] subDirectories = parentDir.listFiles(File::isDirectory);
            if (subDirectories != null) {
                // Cr�er un tableau JSON des r�pertoires
                StringBuilder favoritesArray = new StringBuilder("[");
                for (File dir : subDirectories) {
                    String dirPath = dir.getAbsolutePath();

                    // V�rifier le s�parateur de chemin appropri� pour le syst�me d'exploitation
                    String separator = File.separator.equals("\\") ? "\\\\" : "/";

                    // �chapper les caract�res sp�ciaux dans le chemin
                    String escapedDirPath = dirPath.replaceAll("\\\\", "\\\\\\\\").replaceAll("/", "\\/");

                    favoritesArray.append("\"").append(escapedDirPath).append("\",");
                }
                favoritesArray.deleteCharAt(favoritesArray.length() - 1); // Supprimer la virgule finale
                favoritesArray.append("]");

                // Cr�er le contenu par d�faut du fichier de configuration
                String defaultConfigContent = "{\"favoris\":" + favoritesArray.toString() + ",\"viewers\":{\"txt\":\"textViewer\",\"jpeg\":\"imageView\",\"png\":\"imageView\",\"pdf\":\"pdfViewer\",\"gif\":\"gifViewer\"}}";
                try {
                    // �crivez le contenu par d�faut dans le fichier
                    FileWriter writer = new FileWriter(configFile);
                    writer.write(defaultConfigContent);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Aucun dossier trouv� dans le r�pertoire courant.");
            }
        }
    }
    
    @Override
    public void run() {
    	
    	// Cr�er le fichier de configuration avec les r�pertoires $home comme favoris
    	createConfigFileIfNotExists();
        
        viewers = loadViewersFromConfig();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // -- Panel pour le titre "Favoris" et la liste des noms de r�pertoires
        JPanel favoritesPanel = new JPanel(new BorderLayout());

        // Titre "Favoris"
        JLabel favoritesLabel = new JLabel("Favoris");
        favoritesLabel.setHorizontalAlignment(SwingConstants.CENTER); // Centrer le texte
        favoritesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Ajouter du padding
        favoritesPanel.add(favoritesLabel, BorderLayout.NORTH);

        // Liste des r�pertoires favoris depuis le fichier de configuration JSON
        String[] favoriteDirectories = getFavoriteDirectories();
        favoritesList = new JList<>(favoriteDirectories);
        favoritesList.addListSelectionListener(e -> {
            String selectedDirectory = favoritesList.getSelectedValue();
            if (selectedDirectory != null) {
                String fullPath = getFavoriteDirectoryPath(selectedDirectory);
                navigationLabel.setText(fullPath);
                updateFilesList(fullPath, 0);
            }
          
        });
        favoritesPanel.add(new JScrollPane(favoritesList), BorderLayout.CENTER);

        // -- Panel pour le contenu du r�pertoire
        JPanel contentPanel = new JPanel(new BorderLayout());

        // Label pour indiquer la navigation courante
        navigationLabel = new JLabel("");
        navigationLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Ajouter du padding
        contentPanel.add(navigationLabel, BorderLayout.NORTH);

        // Panel pour afficher le contenu du r�pertoire
        JPanel columnsPanel = new JPanel(new GridLayout(1, 3)); // Utilisation de GridLayout 1 ligne 3 colonnes

        // Initialisation des listes vides dans chaque colonne
        for (int i = 0; i < 3; i++) {
            JList<String> fileList = new JList<>(new String[]{});
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && !e.isShiftDown() && !e.isControlDown()) {
                        int selectedIndex = fileList.locationToIndex(e.getPoint());
                        fileList.setSelectedIndex(selectedIndex);
                    }
                }
            });
            JScrollPane scrollPane = new JScrollPane(fileList);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            fileLists.add(fileList);
            columnsPanel.add(scrollPane);
        }

        // Utilisation d'un JScrollPane pour le panel des colonnes
        columnsScrollPane = new JScrollPane(columnsPanel);
        columnsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); // Supprimer la barre de d�filement vertical

        contentPanel.add(columnsScrollPane, BorderLayout.CENTER);

        // Utilisation du JSplitPane pour s�parer les panels de favoris et de contenu
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, favoritesPanel, contentPanel);
        splitPane.setResizeWeight(0.2); // D�finir le poids de redimensionnement

        // -- Panel pour la visualisation du fichier
        JPanel fileViewerPanel = new JPanel(new BorderLayout());

        // Label pour afficher le nom du fichier
        fileNameLabel = new JLabel("Nom du fichier");
        fileNameLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Ajouter du padding
        fileViewerPanel.add(fileNameLabel, BorderLayout.NORTH);

        // Panel pour afficher le contenu du fichier
        fileContentViewerPanel = new JPanel(new BorderLayout()); // Utilisation de BorderLayout
        fileContentViewerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK)); // Ajoute une bordure noire
        // Ajoutez le panneau de contenu du fichier � votre panneau de visualisation
        fileViewerPanel.add(fileContentViewerPanel, BorderLayout.CENTER);

        // Panel pour afficher la taille du fichier et la date
        JPanel fileInfoPanel = new JPanel(new GridLayout(2, 1));

        // Label pour afficher la taille du fichier
        fileSizeLabel = new JLabel("Taille : ");
        fileSizeLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0)); // Ajouter du padding
        fileInfoPanel.add(fileSizeLabel);

        // Label pour afficher la date du fichier
        fileDateLabel = new JLabel("Date : ");
        fileDateLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 3, 0)); // Ajouter du padding
        fileInfoPanel.add(fileDateLabel);

        // Ajouter le panel d'informations sur le fichier au panel de visualisation
        fileViewerPanel.add(fileInfoPanel, BorderLayout.SOUTH);

        // Ajouter le panel de visualisation � droite du JSplitPane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, splitPane, fileViewerPanel);
        mainSplitPane.setResizeWeight(0.8); // D�finir le poids de redimensionnement

       
        // Ajout du menu de configuration
        JMenuBar menuBar = new JMenuBar();
        JMenu configMenu = new JMenu("Configuration");
        JMenuItem editItem = new JMenuItem("Modifier");
        editItem.addActionListener(e -> {
            ConfigurationWindow configWindow = new ConfigurationWindow(viewers, this);
            configWindow.setVisible(true);
        });
        configMenu.add(editItem);
        menuBar.add(configMenu);
        
        // Ajout du menu Rechercher
        JMenu searchMenu = new JMenu("Rechercher");
        JMenuItem searchMenuItem = new JMenuItem("Ouvrir la recherche");
        searchMenuItem.addActionListener(e -> {
        	
        	createSearchPanel();

            // Cr�ez un nouveau JSplitPane pour contenir le panel de visualisation du fichier et le panel de recherche
            JSplitPane splitPaneWithSearch = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, fileViewerPanel, searchPanel);
            splitPaneWithSearch.setResizeWeight(0.8); // R�glez le poids de redimensionnement
            splitPaneWithSearch.setDividerLocation(0.8); // R�glez la position du diviseur

            // Remplacez le panel de visualisation du fichier actuel par le nouveau JSplitPane
            mainSplitPane.setRightComponent(splitPaneWithSearch);
        });
        searchMenu.add(searchMenuItem);
        
        // Ajout de l'option pour fermer la recherche
        JMenuItem closeSearchMenuItem = new JMenuItem("Fermer la recherche");
        closeSearchMenuItem.addActionListener(e -> {
            // Supprimer la partie de recherche du panel principal
            mainSplitPane.setRightComponent(fileViewerPanel);
        });
        searchMenu.add(closeSearchMenuItem);
        
        menuBar.add(searchMenu);

        
        frame.setJMenuBar(menuBar);

        frame.add(mainSplitPane, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }

    private void createSearchPanel() {
        // -- Panel pour la partie de recherche
        searchPanel = new JPanel(new BorderLayout());
        
        // Panel pour le titre et le champ de recherche
        JPanel searchHeaderPanel = new JPanel(new BorderLayout());
        
        JTextField searchField = new JTextField();
        searchTitleLabel = new JLabel("Recherche");
        
        searchTitleLabel.setHorizontalAlignment(SwingConstants.CENTER); // Centrer le texte
        searchTitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Ajouter du padding
        searchHeaderPanel.add(searchTitleLabel, BorderLayout.NORTH);
       
        searchHeaderPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchHeaderPanel, BorderLayout.NORTH); // Ajoutez le panel de titre et de champ de recherche au panel de recherche principal
        
        // Cr�ez un composant JList pour afficher les r�sultats de recherche
        searchResultsList = new JList<>();
        searchPanel.add(new JScrollPane(searchResultsList), BorderLayout.CENTER);
        
        // Ajoutez un ListSelectionListener pour mettre � jour la navigation en fonction du fichier s�lectionn� dans les r�sultats de recherche
        searchResultsList.addListSelectionListener(e -> {
            String selectedFile = searchResultsList.getSelectedValue();
            
            String directoryPath = "";
      
	        
            if (selectedFile != null) {
            	
            	// Recherchez le chemin du fichier s�lectionn� dans la liste filePaths
                for (String filePath : filePaths) {
                    if (filePath.endsWith(selectedFile))  {
                   
                        directoryPath = filePath;
                        break;
                    }
                }
                
            	
                File selected = new File(directoryPath);
             
                updateFileInfo(selected);
                // Mettre � jour la navigation en fonction du fichier s�lectionn�
                updateNavigation(directoryPath);
            }
            
        });
   
        // Ajoutez un ActionListener pour ex�cuter la recherche lorsque l'utilisateur appuie sur Entr�e dans le champ de recherche
        searchField.addActionListener(e -> {
        	// Clear the filePaths list
            filePaths.clear();
            String searchTerm = searchField.getText();
            String directoryPath = navigationLabel.getText();
            // Ex�cutez la recherche en arri�re-plan
            FileSearchWorker worker = new FileSearchWorker(directoryPath, searchTerm);
            worker.execute();
        });
        
        System.out.println(navigationLabel.getText());

    }
    
    
    private void updateNavigation(String filePath) {
        // Cr�er un objet File pour le chemin du fichier
        File file = new File(filePath);
        // Obtenez le chemin du r�pertoire parent
        String directoryPath = file.getParent();
        
        // Parcourez la liste des favoris pour s�lectionner le r�pertoire favori correspondant
        String selectedFavorite = null;
        for (int i = 0; i < favoritesList.getModel().getSize(); i++) {
            String favorite = favoritesList.getModel().getElementAt(i);
            if (directoryPath.contains(favorite)) {
                selectedFavorite = favorite;
                break;
            }
        }
        
        if (selectedFavorite != null) {
            for (int i = 0; i < favoritesList.getModel().getSize(); i++) {
                if (favoritesList.getModel().getElementAt(i).equals(selectedFavorite)) {
                    favoritesList.setSelectedIndex(i);
                    break;
                }
            }
        }
   
     // Parcourez les �l�ments de la premi�re colonne
        for (int i = 0; i < fileLists.get(0).getModel().getSize(); i++) {
            String listItem = fileLists.get(0).getModel().getElementAt(i);
            // V�rifiez si le chemin du fichier s�lectionn� contient l'�l�ment de la premi�re colonne
            if (filePath.contains(listItem)) {
                // S�lectionnez cet �l�ment dans la premi�re colonne
                fileLists.get(0).setSelectedIndex(i);
                // R�cup�rez l'index de l'�l�ment s�lectionn� dans la premi�re colonne
                int startIndex = filePath.indexOf(listItem) + listItem.length() + 1;
                // Assurez-vous que l'index est valide avant d'appeler substring
                if (startIndex < filePath.length()) {
                    // R�cup�rez le chemin relatif � partir de l'�l�ment s�lectionn� dans la premi�re colonne
                    String relativePath = filePath.substring(startIndex);
                    // Appelez une m�thode r�cursive pour s�lectionner les �l�ments dans les colonnes suivantes
                    selectItemInNextColumn(relativePath, 0);
                }
                break; // Sortez de la boucle une fois que l'�l�ment a �t� trouv� et s�lectionn�
            }
        }

        
    }

    // M�thode r�cursive pour s�lectionner les �l�ments dans les colonnes suivantes
    private void selectItemInNextColumn(String path, int columnIndex) {
        if (columnIndex < fileLists.size() - 1) {
            // Parcourez les �l�ments de la colonne suivante
            for (int i = 0; i < fileLists.get(columnIndex + 1).getModel().getSize(); i++) {
                String listItem = fileLists.get(columnIndex + 1).getModel().getElementAt(i);
                // V�rifiez si le chemin restant contient l'�l�ment de la colonne suivante
                int index = path.indexOf(listItem);
                if (index != -1) {
                    // S�lectionnez cet �l�ment dans la colonne suivante
                    fileLists.get(columnIndex + 1).setSelectedIndex(i);
                    // R�cursivement, appelez cette m�thode pour la colonne suivante avec le reste du chemin
                    if (index + listItem.length() + 1 < path.length()) {
                        selectItemInNextColumn(path.substring(index + listItem.length() + 1), columnIndex + 1);
                    }
                    break;
                }
            }
        }
    }





   

    // M�thode pour obtenir les noms des r�pertoires favoris depuis le fichier de configuration JSON
    private String[] getFavoriteDirectories() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Sp�cifiez l'encodage UTF-8 lors de la lecture du fichier
            FileInputStream fileInputStream = new FileInputStream(configFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            
            // Utilisez InputStreamReader pour lire le fichier avec l'encodage UTF-8
            ExplorerConfig config = mapper.readValue(inputStreamReader, ExplorerConfig.class);
            return config.getFavoris().stream()
                    .map(path -> new File(path).getName())
                    .toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[]{};
        }
    }


    // M�thode pour obtenir le chemin complet d'un r�pertoire favori � partir de son nom
    private String getFavoriteDirectoryPath(String directoryName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Sp�cifiez l'encodage UTF-8 lors de la lecture du fichier
            FileInputStream fileInputStream = new FileInputStream(configFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            
            // Utilisez InputStreamReader pour lire le fichier avec l'encodage UTF-8
            ExplorerConfig config = mapper.readValue(inputStreamReader, ExplorerConfig.class);
            for (String path : config.getFavoris()) {
                if (new File(path).getName().equals(directoryName)) {
                    return path;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // M�thode pour mettre � jour la liste des fichiers/r�pertoires dans la premi�re colonne
    private void updateFilesList(String directoryPath, int columnIndex) {
        // Effacer le contenu des colonnes � droite si la s�lection a lieu dans une colonne � gauche
        for (int i = columnIndex + 1; i < fileLists.size(); i++) {
            fileLists.get(i).setListData(new String[]{});
        }

        // Supprimer les colonnes suppl�mentaires si la s�lection a lieu dans les colonnes 1 ou 2
        if (columnIndex <= 1) {
            int columnsToRemove = fileLists.size() - 3;
            for (int i = 0; i < columnsToRemove; i++) {
                removeLastColumn();
            }
        }

        // Mettre � jour le label de navigation
        navigationLabel.setText(directoryPath);

        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            String[] fileNames = Arrays.stream(files)
                    .map(File::getName)
                    .toArray(String[]::new);
            fileLists.get(columnIndex).setListData(fileNames);

            // Ajouter un ListSelectionListener pour g�rer la s�lection dans cette colonne
            fileLists.get(columnIndex).addListSelectionListener(e -> {
                String selectedFile = fileLists.get(columnIndex).getSelectedValue();
                if (selectedFile != null) {
                    File selected = new File(directory, selectedFile);
                    if (selected.isDirectory()) {
                        // Supprimer les colonnes suppl�mentaires si la s�lection a lieu dans les colonnes 1 ou 2
                        if (columnIndex <= 1) {
                            int columnsToRemove = fileLists.size() - 3;
                            for (int i = 0; i < columnsToRemove; i++) {
                                removeLastColumn();
                            }
                        } else {
                            // V�rifier si les colonnes suppl�mentaires � droite sont vides et les supprimer le cas �ch�ant
                            for (int i = fileLists.size() - 1; i > columnIndex + 1; i--) {
                                JList<String> columnList = fileLists.get(i);
                                if (columnList.getModel().getSize() == 0) {
                                    removeLastColumn();
                                } else {
                                    break; // Sortir de la boucle d�s qu'une colonne non vide est trouv�e
                                }
                            }
                        }

                        // Mettre � jour le contenu dans la colonne � droite ou cr�er une nouvelle colonne
                        if (columnIndex < fileLists.size() - 1) {
                            updateFilesList(selected.getPath(), columnIndex + 1);
                        } else {
                            addNewColumn();
                            updateFilesList(selected.getPath(), columnIndex + 1);
                        }
                    }
                }
            });

            fileLists.get(columnIndex).addListSelectionListener(e -> {
                String selectedFile = fileLists.get(columnIndex).getSelectedValue();
                if (selectedFile != null) {
                    File selected = new File(directory, selectedFile);
                    if (selected.isFile()) {
                        // Mettre � jour les informations du fichier s�lectionn�
                        updateFileInfo(selected);
                    } else if (selected.isDirectory()) {
                        // Votre logique pour le traitement des r�pertoires si n�cessaire
                    }
                }
            });
        }
    }

    // M�thode pour afficher le contenu d'un fichier texte
    private void showTextFileContent(File file) {
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
            bufferedReader.close();
            JTextArea textArea = new JTextArea(content.toString());
            textArea.setEditable(false);
            fileContentViewerPanel.removeAll();
            fileContentViewerPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            fileContentViewerPanel.revalidate();
            fileContentViewerPanel.repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // M�thode pour afficher le contenu d'un fichier PDF
    private void showPdfFileContent(File file) {
        try {
            PDDocument document = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 100);
            JLabel label = new JLabel(new ImageIcon(image));
            fileContentViewerPanel.removeAll();
            fileContentViewerPanel.add(new JScrollPane(label), BorderLayout.CENTER);
            fileContentViewerPanel.revalidate();
            fileContentViewerPanel.repaint();
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // M�thode pour afficher une image
    private void showImage(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            ImageIcon icon = new ImageIcon(image);
            JLabel label = new JLabel(icon);
            fileContentViewerPanel.removeAll();
            fileContentViewerPanel.add(new JScrollPane(label), BorderLayout.CENTER);
            fileContentViewerPanel.revalidate();
            fileContentViewerPanel.repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void showGIF(File file) {
        try {
            ImageIcon gifIcon = new ImageIcon(file.toURI().toURL());
            JLabel gifLabel = new JLabel(gifIcon);

            // Cr�er un JPanel pour afficher le fichier GIF
            JPanel gifPanel = new JPanel(new BorderLayout());
            gifPanel.add(new JScrollPane(gifLabel), BorderLayout.CENTER);

            // Ajouter le JPanel au panneau de visualisation du fichier
            fileContentViewerPanel.removeAll();
            fileContentViewerPanel.add(gifPanel, BorderLayout.CENTER);
            fileContentViewerPanel.revalidate();
            fileContentViewerPanel.repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    

    // M�thode pour afficher les informations d'un fichier
    private void updateFileInfo(File file) {
        fileNameLabel.setText(file.getName());
        fileSizeLabel.setText("Taille : " + (file.length() / 1024) + " KB");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String lastModified = dateFormat.format(new Date(file.lastModified()));
        fileDateLabel.setText("Date : " + lastModified);
        String fileExtension = getFileExtension(file.getName());
        if (viewers.containsKey(fileExtension)) {
            String viewer = viewers.get(fileExtension);
            if (viewer.equals("textViewer")) {
                showTextFileContent(file);
            } else if (viewer.equals("pdfViewer")) {
                showPdfFileContent(file);
            } else if (viewer.equals("imageView")){
            	showImage(file);
               
            }  else if (viewer.equals("gifViewer")){
            	showGIF(file);
            }  else {
            	 // Si le type de fichier n'est pas pris en charge
            	 fileContentViewerPanel.removeAll();
                 fileContentViewerPanel.revalidate();
                 fileContentViewerPanel.repaint();
            }
        } else {
        	 fileContentViewerPanel.removeAll();
             fileContentViewerPanel.revalidate();
             fileContentViewerPanel.repaint();
        }
    }
     
    // M�thode pour ajouter une nouvelle colonne � la liste des fichiers
    private void addNewColumn() {
        JList<String> fileList = new JList<>(new String[]{});
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && !e.isShiftDown() && !e.isControlDown()) {
                    int selectedIndex = fileList.locationToIndex(e.getPoint());
                    fileList.setSelectedIndex(selectedIndex);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fileLists.add(fileList);
        columnsScrollPane.setViewportView(createColumnsPanel());
        frame.revalidate();
        frame.repaint();
    }

    // M�thode pour supprimer la derni�re colonne de la liste des fichiers
    private void removeLastColumn() {
        if (fileLists.size() > 3) {
            fileLists.remove(fileLists.size() - 1);
            columnsScrollPane.setViewportView(createColumnsPanel());
            frame.revalidate();
            frame.repaint();
        }
    }

    // M�thode pour cr�er le panel des colonnes avec les listes de fichiers
    private JPanel createColumnsPanel() {
        JPanel columnsPanel = new JPanel(new GridLayout(1, fileLists.size()));
        for (JList<String> fileList : fileLists) {
            JScrollPane scrollPane = new JScrollPane(fileList);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            columnsPanel.add(scrollPane);
        }
        return columnsPanel;
    }

    // M�thode pour obtenir l'extension d'un fichier
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    // M�thode pour charger les visionneuses de fichiers � partir du fichier de configuration JSON
    private Map<String, String> loadViewersFromConfig() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Sp�cifiez l'encodage UTF-8 lors de la lecture du fichier
            FileInputStream fileInputStream = new FileInputStream(configFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            
            // Utilisez InputStreamReader pour lire le fichier avec l'encodage UTF-8
            ExplorerConfig config = mapper.readValue(inputStreamReader, ExplorerConfig.class);
            return config.getViewers();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    
    public void updateFavoritesList(String[] favoriteDirectories) {
    	
        favoritesList.setListData(favoriteDirectories);
    }
    

    // Travailleur SwingWorker pour effectuer la recherche de fichiers en arri�re-plan
    private class FileSearchWorker extends SwingWorker<List<String>, Void> {
        private String directoryPath;
        private String searchTerm;

        public FileSearchWorker(String directoryPath, String searchTerm) {
            this.directoryPath = directoryPath;
            this.searchTerm = searchTerm;
        }

        
        @Override
        protected List<String> doInBackground() throws Exception {
            List<String> searchResults = new ArrayList<>();
            String command = "cmd /c dir /B /A-D /S \"" + directoryPath + "\" | findstr /R /I /C:\"" + searchTerm + "\"";
            
            // Execute the command
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP850")); //encodage

            
            // Read the output of the command
            String line;
            while ((line = reader.readLine()) != null) {
            	
                
                // Extract the filename from the full path
                String fileName = new File(line).getName();
                // Check if the filename matches the search term
                if (fileName.toLowerCase().contains(searchTerm.toLowerCase())) {
                    // Add the file name to the searchResults list
                    searchResults.add(fileName);
                    
                    // Add the file path to the list
                    filePaths.add(line);
                }
            }
            
            // Wait for the process to finish
            process.waitFor();
            
            // Close the reader
            reader.close();
            
            return searchResults;
        }


        @Override
        protected void done() {
            try {
                List<String> searchResults = get();
                DefaultListModel<String> model = new DefaultListModel<>();
                for (String result : searchResults) {
                    model.addElement(result);
                }
                searchResultsList.setModel(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    // M�thode principale pour ex�cuter l'application
    public static void main(String[] args) {
        JFrame frame = new JFrame("Explorateur de fichiers");
        SwingUtilities.invokeLater(new FileExplorerUI(frame));
    }
}




