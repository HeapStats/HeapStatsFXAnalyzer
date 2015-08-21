/*
 * Copyright (C) 2014-2015 Yasumasa Suenaga
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package jp.co.ntt.oss.heapstats.plugin.builtin.reftree;

import jp.co.ntt.oss.heapstats.snapshot.ReferenceTracker;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxGraphTransferable;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import jp.co.ntt.oss.heapstats.WindowController;
import jp.co.ntt.oss.heapstats.container.snapshot.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.SnapShotController;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;

/**
 * FXML Controller class of Reference Data tab.
 *
 * @author Yasumasa Suenaga
 */
public class RefTreeController extends PluginController implements Initializable, MouseListener {

    /** Reference to the region for displaying the object. */
    private ReferenceGraph graph;

    /** Reference to the enclosing graph component. */
    private mxGraphComponent graphComponent;
    
    private SnapShotController snapShotController;
    
    private LocalDateTime snapShotDate;
    
    private Map<Long, ObjectData> snapShot;
    
    private long startTag;
    
    @FXML
    private AnchorPane topAnchorPane;
    
    @FXML
    private Label snapshotLabel;
    
    @FXML
    private RadioButton radioParent;
    
    @FXML
    private RadioButton radioSize;
    
    @FXML
    private CheckBox rankCheckBox;
    
    @FXML
    private SwingNode graphNode;
    
    /**
     * Effectively callback method when this plugin is selected.
     * 
     * @param event Event which is triggered to call this method.
     */
    private void buildTab(Event event){
        Tab thisTab = (Tab)event.getTarget();
        if(!thisTab.isSelected()){
            return;
        }
        
        try{
            snapShot = snapShotController.getSelectedSnapShot();
        }
        catch(Exception e){
            ResourceBundle resource = ResourceBundle.getBundle("reftreeResources", new Locale(HeapStatsUtils.getLanguage()));
            Alert dialog = new Alert(AlertType.ERROR);
            dialog.setTitle("Error");
            dialog.setHeaderText(resource.getString("buildtab.snapshot.message"));
            TextArea stackArea = new TextArea(HeapStatsUtils.stackTarceToString(e));
            stackArea.setEditable(false);
            dialog.getDialogPane().setExpandableContent(stackArea);
            dialog.showAndWait();
            return;
        }
        
        try{
            snapShotDate = snapShotController.getSelectedSnapShotHeader().getSnapShotDate();
        }
        catch(Exception e){
            ResourceBundle resource = ResourceBundle.getBundle("reftreeResources", new Locale(HeapStatsUtils.getLanguage()));
            Alert dialog = new Alert(AlertType.ERROR);
            dialog.setTitle("Error");
            dialog.setHeaderText(resource.getString("buildtab.datetime.message"));
            TextArea stackArea = new TextArea(HeapStatsUtils.stackTarceToString(e));
            stackArea.setEditable(false);
            dialog.getDialogPane().setExpandableContent(stackArea);
            dialog.showAndWait();
            return;
        }
        
        try{
            startTag = snapShotController.getSelectedClassTag();
        }
        catch(IllegalStateException e){
            HeapStatsUtils.showExceptionDialog(e);
            return;
        }
        
        snapshotLabel.setText((new LocalDateTimeConverter()).toString(snapShotDate));
        
        SwingUtilities.invokeLater(() -> initializeSwingNode());
    }
    
    /**
     * Initialize method for SwingNode.
     * This method is called by Swing Event Dispatcher Thread.
     */
    private void initializeSwingNode(){
        graph = new ReferenceGraph();
        graph.setCellsEditable(false);

        graph.getModel().beginUpdate();
        {
            ObjectData data = snapShot.get(startTag);
            ReferenceCell cell = new ReferenceCell(data, true, false);
            graph.addCell(cell);
        }
        graph.getModel().endUpdate();

        graph.setEdgeStyle();

        graphComponent = new mxGraphComponent(graph);
        graphComponent.getGraphControl().addMouseListener(this);
        graphComponent.setToolTips(true);
        
        graphNode.setContent(graphComponent);
        
        /*
         * FIXME!
         * SwingNode is not shown immediately.
         * So I call repaint() method at last and request layout() call.
         */        
        graphComponent.repaint();
        Platform.runLater(() -> topAnchorPane.layout());
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        snapShotController = (SnapShotController)WindowController.getInstance().getPluginController("SnapShot Data");
        
        if(snapShotController == null){
            throw new IllegalStateException(rb.getString("initialize.failed.message"));
        }
        
        /*
         * Initialize SwingNode with dummy mxGraphComponent.
         * SwingNode seems to hook focus ungrab event.
         * If SwingNode is empty, IllegalArgumentException with "null source" is
         * thrown when another stage (e.g. about dialog) is shown.
         * To avoid this, dummy mxGraphComponent is set to SwingNode at first.
         */
        SwingUtilities.invokeLater(() -> {
                                            mxGraphComponent dummyGraphComponent = new mxGraphComponent(new ReferenceGraph());
                                            graphNode.setContent(dummyGraphComponent);
                                         });
        
    }    

    @Override
    public String getPluginName() {
        return "Reference Data";
    }

    @Override
    public EventHandler<Event> getOnPluginTabSelected() {
        return (event -> buildTab(event));
    }
    
    /**
     * Add reference cells to graph.
     * This method creates cell which represents child, and connects to it from parent.
     * 
     * @param parentCell Parent cell
     * @param child Child data.
     */
    private void addReferenceCell(ReferenceCell parentCell, ObjectData objData){
        ReferenceCell cell = null;
        mxCell tmp = (mxCell)parentCell.getParent();
                                       
        for(int i = 0; i < tmp.getChildCount(); i++){
            mxICell target = tmp.getChildAt(i);
                                         
            if(target.isVertex() && (((ReferenceCell)target).getTag() == objData.getTag())){
                cell = (ReferenceCell)target;
            }
                                         
        }

        ReferenceCell edge = new ReferenceCell(objData, false, true);

        if(cell == null){
            cell = new ReferenceCell(objData, false, false);
            graph.addCell(cell);
        }
                                       
        long size = objData.getTotalSize() / 1024; // KB
        
        edge.setValue((size == 0) ? objData.getCount() + "\n< 1"
                                  : objData.getCount() + "\n" + objData.getTotalSize() / 1024); // KB
                                       
        graph.addEdge(edge, graph.getDefaultParent(), parentCell, cell, 1);
    }

    /**
     * Displayed on the graph to get a child or of a cell object you clicked,
     * the information of the parent.
     *
     * @param parentCell Cell object you clicked
     */
    private void drawMind(ReferenceCell parentCell) {
        
        if((parentCell.getEdgeCount() > 1) ||
           (parentCell.isRoot() && (parentCell.getEdgeCount() > 0)) ||
           (parentCell.isEdge())){
            return;
        }

        OptionalInt rankLevel = rankCheckBox.isSelected() ? OptionalInt.of(HeapStatsUtils.getRankLevel())
                                                          : OptionalInt.empty();
        ReferenceTracker refTracker = new ReferenceTracker(snapShot, rankLevel, Optional.empty());
        List<ObjectData> objectList = radioParent.isSelected() ? refTracker.getParents(parentCell.getTag(), radioSize.isSelected())
                                                               : refTracker.getChildren(parentCell.getTag(), radioSize.isSelected());

        if(objectList.isEmpty()){
            return;
        }

        graph.getModel().beginUpdate();
        {
            objectList.forEach(o -> addReferenceCell(parentCell, o));

            if(mxGraphTransferable.dataFlavor == null){
                try{
                    mxGraphTransferable.dataFlavor = new DataFlavor(
                        DataFlavor.javaJVMLocalObjectMimeType + "; class=com.mxgraph.swing.util.mxGraphTransferable",
                        null, new mxGraphTransferable(null, null).getClass().getClassLoader());
                }
                catch(ClassNotFoundException e){
                  // do nothing
                }
            }

            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
            layout.execute(graph.getDefaultParent());
        }
        graph.getModel().endUpdate();
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        Object cell = graphComponent.getCellAt(e.getX(), e.getY());
            
        if((e.getButton() == MouseEvent.BUTTON1) && (cell != null) && (cell instanceof ReferenceCell)){
            drawMind((ReferenceCell)cell);
        }
            
        graphComponent.repaint();
        Platform.runLater(() -> topAnchorPane.layout());
   }

    @Override
    public void mousePressed(MouseEvent e) {
        /* Nothing to do */
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        /* Nothing to do */
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        /* Nothing to do */
    }

    @Override
    public void mouseExited(MouseEvent e) {
        /* Nothing to do */
    }
    
    /**
     * Event handler of OK button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOkClick(ActionEvent event){
        mxCell cell = (mxCell)graph.getDefaultParent();
        List<Object> removeCells = new ArrayList<>();

        for(int i = 0; i < cell.getChildCount(); i++){
            mxCell removeCell = (mxCell)cell.getChildAt(i);
            
            if(removeCell.isEdge() || !((ReferenceCell)removeCell).isRoot()){
                removeCells.add(removeCell);
            }
            else{
                graph.removeSelectionCell(removeCell);
                mxGeometry geometry = ((ReferenceCell)removeCell).getGeometry();
                geometry.setX(0);
                geometry.setY(0);
                ((ReferenceCell)removeCell).setGeometry(geometry);
            }
            
        }
        
        graph.removeCells(removeCells.toArray());
        graph.refresh();
        graphComponent.refresh();
        graphComponent.getViewport().setViewPosition(new Point(0, 0));
    }

    @Override
    public String getLicense() {
        return PluginController.LICENSE_GPL_V2;
    }

    @Override
    public Map<String, String> getLibraryLicense() {
        Map<String, String> licenseMap = new HashMap<>();
        licenseMap.put("JGraphX", PluginController.LICENSE_BSD);
        
        return licenseMap;
    }

    @Override
    public Runnable getOnCloseRequest() {
        return null;
    }
    
}
