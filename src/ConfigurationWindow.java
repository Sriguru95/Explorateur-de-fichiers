import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("serial")
public class ConfigurationWindow extends JFrame {
	
	private FileExplorerUI fileExplorerUI;

    private JTextField directoryField;
    private DefaultListModel<String> favoritesListModel;
    private List<JLabel> extensionLabelList;
    private List<JComboBox<String>> viewerComboBoxList;
    
    private List<String> tempFavoriteList;                                          
    private String selectedDirectoryPath = ""; // Déclarer une variable globale pour stocker le chemin du répertoire sélectionné

    private Map<String, String> viewers;

    public ConfigurationWindow(Map<String, String> viewers, FileExplorerUI fileExplorerUI) {
        this.viewers = viewers;
        this.fileExplorerUI = fileExplorerUI; // Initialiser la référence à la fenêtre principale
        setTitle("Configuration");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        extensionLabelList = new ArrayList<>();
        viewerComboBoxList = new ArrayList<>();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Première zone
        JPanel directoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton chooseDirectoryButton = new JButton("Choisir un répertoire");
        chooseDirectoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseDirectory();
            }
        });
        directoryField = new JTextField(20);
        directoryField.setEditable(false);
        JButton addButton = new JButton("Ajouter");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFavoriteDirectory(directoryField.getText());
            }
        });
        directoryPanel.add(chooseDirectoryButton);
        directoryPanel.add(directoryField);
        directoryPanel.add(addButton);
        panel.add(directoryPanel);

      
        // Deuxième zone
        JPanel favoritesPanel = new JPanel(new BorderLayout());
        JLabel favoritesLabel = new JLabel("Favoris", SwingConstants.CENTER); // Centrer le label horizontalement
        favoritesPanel.add(favoritesLabel, BorderLayout.NORTH);

        // Récupérer les noms des répertoires favoris depuis la configuration
        String[] favoriteDirectories = getFavoriteDirectories();
        favoritesListModel = new DefaultListModel<>();
        for(String dir : favoriteDirectories) {
            favoritesListModel.addElement(new File(dir).getName()); // Ajouter seulement les noms des répertoires
        }
        JList<String> favoritesList = new JList<>(favoritesListModel);
        JScrollPane favoritesScrollPane = new JScrollPane(favoritesList);
        favoritesPanel.add(favoritesScrollPane, BorderLayout.CENTER);

        panel.add(favoritesPanel);


        // Troisième zone
        JPanel associationsPanel = new JPanel(new GridLayout(0, 2)); // GridLayout avec 2 colonnes
        associationsPanel.setBorder(BorderFactory.createTitledBorder("Associations")); // Ajout d'une bordure titrée
        // Récupérer les extensions de fichier et les visualiseurs associés depuis la configuration
        ExplorerConfig config = getConfiguration();
        for (Map.Entry<String, String> entry : config.getViewers().entrySet()) {
            JLabel extensionLabel = new JLabel(entry.getKey()); // Créer un label pour l'extension de fichier
            associationsPanel.add(extensionLabel);
            JComboBox<String> viewerComboBox = new JComboBox<>(new String[]{"textViewer", "imageView", "pdfViewer", "gifViewer"});
            viewerComboBox.setSelectedItem(entry.getValue()); // Sélectionner le visualiseur associé depuis la configuration
            associationsPanel.add(viewerComboBox);
            extensionLabelList.add(extensionLabel);
            viewerComboBoxList.add(viewerComboBox); 
        }

        panel.add(associationsPanel);

        // Quatrième zone
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("Enregistrer");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfiguration();
                setVisible(false);
            }
        });
        JButton cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonsPanel.add(saveButton);
        buttonsPanel.add(cancelButton);
        panel.add(buttonsPanel);

        add(panel);

        pack(); // Ajuste la taille de la fenêtre en fonction de son contenu
        setLocationRelativeTo(null); // Centre la fenêtre sur l'écran
        setVisible(true); // Rend la fenêtre visible
        
        // Initialisation de la liste temporaire des favoris
        tempFavoriteList = new ArrayList<>();
    }
    
 // Méthode pour obtenir les noms des répertoires favoris depuis la configuration
    private String[] getFavoriteDirectories() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Spécifiez l'encodage UTF-8 lors de la lecture depuis le fichier
            FileInputStream fileInputStream = new FileInputStream(configFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            
            // Utilisez InputStreamReader pour lire depuis le fichier avec l'encodage UTF-8
            ExplorerConfig config = mapper.readValue(inputStreamReader, ExplorerConfig.class);
            return config.getFavoris().toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[]{};
        }
    }


    private void chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedDirectoryPath = selectedFile.getAbsolutePath(); // Sauvegarder le chemin complet
            directoryField.setText(selectedFile.getName()); // Afficher le nom du répertoire dans le champ de texte
        }
    }

    private void addFavoriteDirectory(String directory) {
        
       // Mettre à jour la liste affichée dans l'interface utilisateur
      favoritesListModel.addElement(new File(directory).getName());
        
      // Ajouter le répertoire à la liste temporaire
      tempFavoriteList.add(selectedDirectoryPath);
    }

    private void saveConfiguration() {
        // Ajouter les répertoires de la liste temporaire à la configuration
        for (String dir : tempFavoriteList) {
            addFavoriteDirectoryToConfig(dir);
        }
        // Enregistrez la configuration dans le fichier explorer.conf
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            ExplorerConfig config = new ExplorerConfig();
            config.setFavoris(Arrays.asList(getFavoriteDirectories())); // Convertir le tableau en liste
            config.setViewers(viewers);
            
            
            // Mettre à jour les associations d'extensions de fichier
            for (int i = 0; i < extensionLabelList.size(); i++) {
                JLabel extensionLabel = extensionLabelList.get(i);
                JComboBox<String> viewerComboBox = viewerComboBoxList.get(i);
                viewers.put(extensionLabel.getText(), (String) viewerComboBox.getSelectedItem());
            }

            config.setViewers(viewers);
            
            ObjectMapper mapper = new ObjectMapper();
            
            // Spécifiez l'encodage UTF-8 lors de l'écriture dans le fichier
            FileOutputStream fileOutputStream = new FileOutputStream(configFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            
            // Utilisez OutputStreamWriter pour écrire dans le fichier avec l'encodage UTF-8
            mapper.writeValue(outputStreamWriter, config);

            // Mettre à jour la liste des favoris dans la fenêtre principale
            List<String> newFavoriteNames = new ArrayList<>(); // Créer une liste locale pour stocker les noms des nouveaux répertoires favoris
            for (String dir : getFavoriteDirectories()) {
                newFavoriteNames.add(new File(dir).getName()); // Ajouter seulement les noms des répertoires
            }
            String[] newFavoriteDirectories = newFavoriteNames.toArray(new String[0]);
            fileExplorerUI.updateFavoritesList(newFavoriteDirectories);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    
    // Méthode pour ajouter un répertoire favori à la configuration
    private void addFavoriteDirectoryToConfig(String directory) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            ExplorerConfig config = mapper.readValue(configFile, ExplorerConfig.class);
            config.getFavoris().add(directory);
            mapper.writeValue(configFile, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Getter pour récupérer le chemin du répertoire sélectionné
    public String getSelectedDirectoryPath() {
        return selectedDirectoryPath;
    }
    
    private ExplorerConfig getConfiguration() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Spécifiez l'encodage UTF-8 lors de la lecture du fichier
            FileInputStream fileInputStream = new FileInputStream(configFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            
            // Utilisez InputStreamReader pour lire le fichier avec l'encodage UTF-8
            return mapper.readValue(inputStreamReader, ExplorerConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new ExplorerConfig(); // Retourner une configuration vide en cas d'erreur
        }
    }
}