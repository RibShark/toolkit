package ennuo.toolkit.windows;

import ennuo.craftworld.ex.SerializationException;
import ennuo.craftworld.types.FileArchive;
import ennuo.toolkit.utilities.FileChooser;
import ennuo.toolkit.utilities.Globals;
import java.io.File;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ArchiveManager extends javax.swing.JDialog {
    private final DefaultListModel archiveModel = new DefaultListModel();
    
    public ArchiveManager(JFrame parent) {
        super(parent, "Archive Manager", true);
        this.setIconImage(new ImageIcon(getClass().getResource("/legacy_icon.png")).getImage());
        this.setResizable(false);
        this.initComponents();
        
        for (FileArchive archive : Globals.archives)
            archiveModel.addElement(archive.file.getAbsolutePath());
        this.archivesList.setModel(this.archiveModel);
        
        this.addButton.addActionListener(e -> {
            File file = FileChooser.openFile("data.farc", "farc", false);
            if (file == null) return;
            
            if (Toolkit.instance.isArchiveLoaded(file) != -1) {
                JOptionPane.showMessageDialog(this, "This archive is already loaded!", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            FileArchive archive = null;
            try { archive = new FileArchive(file); }
            catch (SerializationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "An error occurred", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Globals.archives.add(archive);
            this.archiveModel.addElement(file.getAbsolutePath());
            
            this.removeButton.setEnabled(true);
            
            Toolkit.instance.updateWorkspace();
        });
        
        this.removeButton.addActionListener(e -> {
            int index = this.archivesList.getSelectedIndex();
            if (index == -1) return;
            
            FileArchive archive = Globals.archives.get(index);
            
            if (archive.shouldSave) {
                int result = JOptionPane.showConfirmDialog(null, "Do you want to save changes before closing this archive?", "Pending changes", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) 
                    archive.save();
            }
            
            if (this.archiveModel.size() - 1 != 0) {
                if (index == 0)
                    this.archivesList.setSelectedIndex(index + 1);
                else
                    this.archivesList.setSelectedIndex(index - 1);   
            }
            
            Globals.archives.remove(index);
            this.archiveModel.removeElementAt(index);
            
            if (this.archiveModel.size() == 0)
                this.removeButton.setEnabled(false);
            
            Toolkit.instance.updateWorkspace();
        });
        
        this.closeButton.addActionListener(e -> this.dispose());
        
        
    }
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        archivesLabel = new javax.swing.JLabel();
        archiveContainer = new javax.swing.JScrollPane();
        archivesList = new javax.swing.JList<>();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Archive Manager");

        archivesLabel.setText("Archives:");

        archivesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        archiveContainer.setViewportView(archivesList);

        addButton.setText("Add");

        removeButton.setText("Remove");

        closeButton.setText("Close");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(archiveContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(closeButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(archivesLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(archivesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(archiveContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addButton)
                    .addComponent(removeButton)
                    .addComponent(closeButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JScrollPane archiveContainer;
    private javax.swing.JLabel archivesLabel;
    private javax.swing.JList<String> archivesList;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables
}
