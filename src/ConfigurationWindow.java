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
    private String selectedDirectoryPath = ""; // D�clarer une variable globale pour stocker le chemin du r�pertoire s�lectionn�

    private Map<String, String> viewers;

    public ConfigurationWindow(Map<String, String> viewers, FileExplorerUI fileExplorerUI) {
        this.viewers = viewers;
        this.fileExplorerUI = fileExplorerUI; // Initialiser la r�f�rence � la fen�tre principale
        setTitle("Configuration");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        extensionLabelList = new ArrayList<>();
        viewerComboBoxList = new ArrayList<>();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Premi�re zone
        JPanel directoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton chooseDirectoryButton = new JButton("Choisir un r�pertoire");
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

      
        // Deuxi�me zone
        JPanel favoritesPanel = new JPanel(new BorderLayout());
        JLabel favoritesLabel = new JLabel("Favoris", SwingConstants.CENTER); // Centrer le label horizontalement
        favoritesPanel.add(favoritesLabel, BorderLayout.NORTH);

        // R�cup�rer les noms des r�pertoires favoris depuis la configuration
        String[] favoriteDirectories = getFavoriteDirectories();
        favoritesListModel = new DefaultListModel<>();
        for(String dir : favoriteDirectories) {
            favoritesListModel.addElement(new File(dir).getName()); // Ajouter seulement les noms des r�pertoires
        }
        JList<String> favoritesList = new JList<>(favoritesListModel);
        JScrollPane favoritesScrollPane = new JScrollPane(favoritesList);
        favoritesPanel.add(favoritesScrollPane, BorderLayout.CENTER);

        panel.add(favoritesPanel);


        // Troisi�me zone
        JPanel associationsPanel = new JPanel(new GridLayout(0, 2)); // GridLayout avec 2 colonnes
        associationsPanel.setBorder(BorderFactory.createTitledBorder("Associations")); // Ajout d'une bordure titr�e
        // R�cup�rer les extensions de fichier et les visualiseurs associ�s depuis la configuration
        ExplorerConfig config = getConfiguration();
        for (Map.Entry<String, String> entry : config.getViewers().entrySet()) {
            JLabel extensionLabel = new JLabel(entry.getKey()); // Cr�er un label pour l'extension de fichier
            associationsPanel.add(extensionLabel);
            JComboBox<String> viewerComboBox = new JComboBox<>(new String[]{"textViewer", "imageView", "pdfViewer", "gifViewer"});
            viewerComboBox.setSelectedItem(entry.getValue()); // S�lectionner le visualiseur associ� depuis la configuration
            associationsPanel.add(viewerComboBox);
            extensionLabelList.add(extensionLabel);
            viewerComboBoxList.add(viewerComboBox); 
        }

        panel.add(associationsPanel);

        // Quatri�me zone
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

        pack(); // Ajuste la taille de la fen�tre en fonction de son contenu
        setLocationRelativeTo(null); // Centre la fen�tre sur l'�cran
        setVisible(true); // Rend la fen�tre visible
        
        // Initialisation de la liste temporaire des favoris
        tempFavoriteList = new ArrayList<>();
    }
    
 // M�thode pour obtenir les noms des r�pertoires favoris depuis la configuration
    private String[] getFavoriteDirectories() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Sp�cifiez l'encodage UTF-8 lors de la lecture depuis le fichier
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
            directoryField.setText(selectedFile.getName()); // Afficher le nom du r�pertoire dans le champ de texte
        }
    }

    private void addFavoriteDirectory(String directory) {
        
       // Mettre � jour la liste affich�e dans l'interface utilisateur
      favoritesListModel.addElement(new File(directory).getName());
        
      // Ajouter le r�pertoire � la liste temporaire
      tempFavoriteList.add(selectedDirectoryPath);
    }

    private void saveConfiguration() {
        // Ajouter les r�pertoires de la liste temporaire � la configuration
        for (String dir : tempFavoriteList) {
            addFavoriteDirectoryToConfig(dir);
        }
        // Enregistrez la configuration dans le fichier explorer.conf
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            ExplorerConfig config = new ExplorerConfig();
            config.setFavoris(Arrays.asList(getFavoriteDirectories())); // Convertir le tableau en liste
            config.setViewers(viewers);
            
            
            // Mettre � jour les associations d'extensions de fichier
            for (int i = 0; i < extensionLabelList.size(); i++) {
                JLabel extensionLabel = extensionLabelList.get(i);
                JComboBox<String> viewerComboBox = viewerComboBoxList.get(i);
                viewers.put(extensionLabel.getText(), (String) viewerComboBox.getSelectedItem());
            }

            config.setViewers(viewers);
            
            ObjectMapper mapper = new ObjectMapper();
            
            // Sp�cifiez l'encodage UTF-8 lors de l'�criture dans le fichier
            FileOutputStream fileOutputStream = new FileOutputStream(configFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            
            // Utilisez OutputStreamWriter pour �crire dans le fichier avec l'encodage UTF-8
            mapper.writeValue(outputStreamWriter, config);

            // Mettre � jour la liste des favoris dans la fen�tre principale
            List<String> newFavoriteNames = new ArrayList<>(); // Cr�er une liste locale pour stocker les noms des nouveaux r�pertoires favoris
            for (String dir : getFavoriteDirectories()) {
                newFavoriteNames.add(new File(dir).getName()); // Ajouter seulement les noms des r�pertoires
            }
            String[] newFavoriteDirectories = newFavoriteNames.toArray(new String[0]);
            fileExplorerUI.updateFavoritesList(newFavoriteDirectories);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    
    // M�thode pour ajouter un r�pertoire favori � la configuration
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
    
    // Getter pour r�cup�rer le chemin du r�pertoire s�lectionn�
    public String getSelectedDirectoryPath() {
        return selectedDirectoryPath;
    }
    
    private ExplorerConfig getConfiguration() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File configFile = new File(System.getProperty("user.home") + File.separator + "explorer.conf");
            
            // Sp�cifiez l'encodage UTF-8 lors de la lecture du fichier
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