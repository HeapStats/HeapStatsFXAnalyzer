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
package jp.co.ntt.oss.heapstats.plugin.builtin.snapshot;

import java.io.File;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import jp.co.ntt.oss.heapstats.WindowController;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.SnapShotHeader;
import jp.co.ntt.oss.heapstats.container.snapshot.SummaryData;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.tabs.HistogramController;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.tabs.SummaryController;
import jp.co.ntt.oss.heapstats.task.CSVDumpGC;
import jp.co.ntt.oss.heapstats.task.CSVDumpHeap;
import jp.co.ntt.oss.heapstats.task.ParseHeader;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;
import jp.co.ntt.oss.heapstats.utils.TaskAdapter;

/**
 * FXML Controller of SnapShot builtin plugin.
 *
 * @author Yasumasa Suenaga
 */
public class SnapShotController extends PluginController implements Initializable {

    @FXML
    private SummaryController summaryController;

    @FXML
    private HistogramController histogramController;

    @FXML
    private ComboBox<SnapShotHeader> startCombo;

    @FXML
    private ComboBox<SnapShotHeader> endCombo;

    @FXML
    private TextField snapshotList;

    @FXML
    private RadioButton radioInstance;

    @FXML
    private ComboBox<SnapShotHeader> snapShotTimeCombo;

    @FXML
    private TableView<Map.Entry<String, String>> snapShotSummaryTable;

    @FXML
    private TableColumn<Map.Entry<String, String>, String> snapShotSummaryKey;

    @FXML
    private TableColumn<Map.Entry<String, String>, String> snapShotSummaryValue;

    @FXML
    private PieChart usagePieChart;

    @FXML
    private TableColumn<ObjectData, String> objColorColumn;

    @FXML
    private TableView<ObjectData> objDataTable;

    @FXML
    private TableColumn<ObjectData, String> objClassNameColumn;

    @FXML
    private TableColumn<ObjectData, String> objClassLoaderColumn;

    @FXML
    private TableColumn<ObjectData, Long> objInstancesColumn;

    @FXML
    private TableColumn<ObjectData, Long> objSizeColumn;

    @FXML
    private Tab histogramTab;

    @FXML
    private Button okBtn;

    private ObjectProperty<ObservableList<SnapShotHeader>> currentTarget;

    private ObjectProperty<SummaryData> summaryData;

    private ObjectProperty<ObservableSet<String>> currentClassNameSet;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, rb);

        summaryData = new SimpleObjectProperty<>();
        summaryController.summaryDataProperty().bind(summaryData);
        currentTarget = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
        summaryController.currentTargetProperty().bind(currentTarget);
        histogramController.currentTargetProperty().bind(currentTarget);
        snapShotTimeCombo.itemsProperty().bind(currentTarget);
        currentClassNameSet = new SimpleObjectProperty<>();
        summaryController.currentClassNameSetProperty().bind(currentClassNameSet);
        histogramController.currentClassNameSetProperty().bind(currentClassNameSet);
        histogramController.instanceGraphProperty().bind(radioInstance.selectedProperty());
        histogramController.snapshotSelectionModelProperty().bind(snapShotTimeCombo.selectionModelProperty());
        histogramController.setDrawRebootSuspectLine(this::drawRebootSuspectLine);

        startCombo.setConverter(new SnapShotHeaderConverter());
        endCombo.setConverter(new SnapShotHeaderConverter());
        snapShotTimeCombo.setConverter(new SnapShotHeaderConverter());

        snapShotSummaryKey.setCellValueFactory(new PropertyValueFactory<>("key"));
        snapShotSummaryValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        objColorColumn.setCellFactory(p -> new TableCell<ObjectData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                String style = Optional.ofNullable((ObjectData) getTableRow().getItem())
                        .filter(o -> histogramController.getTopNChart().getData().stream().anyMatch(d -> d.getName().equals(o.getName())))
                        .map(o -> "-fx-background-color: " + ChartColorManager.getNextColor(o.getName()))
                        .orElse("-fx-background-color: transparent;");
                setStyle(style);
            }
        });

        objClassNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        objClassLoaderColumn.setCellValueFactory(new PropertyValueFactory<>("loaderName"));
        objInstancesColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        objInstancesColumn.setSortType(TableColumn.SortType.DESCENDING);
        objSizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
        objSizeColumn.setSortType(TableColumn.SortType.DESCENDING);

        okBtn.disableProperty().bind(startCombo.getSelectionModel().selectedIndexProperty().greaterThanOrEqualTo(endCombo.getSelectionModel().selectedIndexProperty()));

        setOnWindowResize((v, o, n) -> Platform.runLater(() -> Stream.of(summaryController.getHeapChart(),
                summaryController.getInstanceChart(),
                summaryController.getGcTimeChart(),
                summaryController.getMetaspaceChart(),
                histogramController.getTopNChart())
                .forEach(c -> Platform.runLater(() -> drawRebootSuspectLine(c)))));

        histogramController.setTaskExecutor(t -> {
            bindTask(t);
            (new Thread(t)).start();
        });
    }

    /**
     * Event handler of SnapShot file button.
     *
     * @param event ActionEvent of this event.
     */
    @FXML
    public void onSnapshotFileClick(ActionEvent event) {
        FileChooser dialog = new FileChooser();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));

        dialog.setTitle(resource.getString("dialog.filechooser.title"));
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("SnapShot file (*.dat)", "*.dat"),
                new ExtensionFilter("All files", "*.*"));

        List<File> snapshotFileList = dialog.showOpenMultipleDialog(WindowController.getInstance().getOwner());

        if (snapshotFileList != null) {
            HeapStatsUtils.setDefaultDirectory(snapshotFileList.get(0).getParent());
            List<String> files = snapshotFileList.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            snapshotList.setText(files.stream().collect(Collectors.joining("; ")));

            TaskAdapter<ParseHeader> task = new TaskAdapter<>(new ParseHeader(files, HeapStatsUtils.getReplaceClassName(), true));
            task.setOnSucceeded(evt -> {
                ObservableList<SnapShotHeader> list = FXCollections.observableArrayList(task.getTask().getSnapShotList());
                startCombo.setItems(list);
                endCombo.setItems(list);
                startCombo.getSelectionModel().selectFirst();
                endCombo.getSelectionModel().selectLast();
            });
            super.bindTask(task);

            Thread parseThread = new Thread(task);
            parseThread.start();
        }

    }

    /**
     * Event handler of OK button.
     *
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOkClick(ActionEvent event) {
        int startIdx = startCombo.getSelectionModel().getSelectedIndex();
        int endIdx = endCombo.getSelectionModel().getSelectedIndex();
        currentTarget.set(FXCollections.observableArrayList(startCombo.getItems().subList(startIdx, endIdx + 1)));
        currentClassNameSet.set(FXCollections.observableSet());
        summaryData.set(new SummaryData(currentTarget.get()));

        Task<Void> topNTask = histogramController.getDrawTopNDataTask(currentTarget.get(), true, null);
        super.bindTask(topNTask);
        Thread topNThread = new Thread(topNTask);
        topNThread.start();

        Task<Void> summarizeTask = summaryController.getCalculateGCSummaryTask(this::drawRebootSuspectLine);
        super.bindTask(summarizeTask);
        Thread summarizeThread = new Thread(summarizeTask);
        summarizeThread.start();
    }

    /**
     * Event handler of SnapShot TIme.
     *
     * @param event ActionEvent of this event.
     */
    @FXML
    @SuppressWarnings("unchecked")
    private void onSnapShotTimeSelected(ActionEvent event) {
        SnapShotHeader header = snapShotTimeCombo.getSelectionModel().getSelectedItem();
        if (header == null) {
            return;
        }

        ObservableList<Map.Entry<String, String>> summaryList = snapShotSummaryTable.getItems();
        summaryList.clear();
        usagePieChart.getData().clear();
        objDataTable.getItems().clear();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));

        summaryList.addAll(
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.date"), (new LocalDateTimeConverter()).toString(header.getSnapShotDate())),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.entries"), Long.toString(header.getNumEntries())),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.instances"), Long.toString(header.getNumInstances())),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.heap"), String.format("%.02f MB", (double) (header.getNewHeap() + header.getOldHeap()) / 1024.0d / 1024.0d)),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.metaspace"), String.format("%.02f MB", (double) (header.getMetaspaceUsage()) / 1024.0d / 1024.0d)),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.cause"), header.getCauseString()),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.gccause"), header.getGcCause()),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.gctime"), String.format("%d ms", header.getGcTime())));

        usagePieChart.getData().addAll(histogramController.getTopNList().get(header.getSnapShotDate()).stream()
                .map(o -> new PieChart.Data(o.getName(), radioInstance.isSelected() ? o.getCount() : o.getTotalSize()))
                .collect(Collectors.toList()));
        usagePieChart.getData().stream()
                .forEach(d -> d.getNode().setStyle("-fx-pie-color: " + ChartColorManager.getNextColor(d.getName())));

        objDataTable.setItems(FXCollections.observableArrayList(
                header.getSnapShot(HeapStatsUtils.getReplaceClassName()).values().stream().collect(Collectors.toList())));
        objDataTable.getSortOrder().add(radioInstance.isSelected() ? objInstancesColumn : objSizeColumn);
    }

    /**
     * Returns plugin name. This value is used to show in main window tab.
     *
     * @return Plugin name.
     */
    @Override
    public String getPluginName() {
        return "SnapShot Data";
    }

    /**
     * Get SnapShot header which is selected. This method returns snapshot
     * header which is selected ins SnapShot Data tab.
     *
     * @return selected snapshot header.
     */
    public SnapShotHeader getSelectedSnapShotHeader() {
        return snapShotTimeCombo.getSelectionModel().getSelectedItem();
    }

    /**
     * Get selected snapshot. This method returns snapshot which is selected in
     * SnapShot Data tab.
     *
     * @return selected snapshot.
     */
    public Map<Long, ObjectData> getSelectedSnapShot() {
        return snapShotTimeCombo.getSelectionModel().getSelectedItem().getSnapShot(HeapStatsUtils.getReplaceClassName());
    }

    /**
     * Get selected object. If histogram tab is active and diff data is
     * selected, this method returns tag which is selected. Other case, this
     * method returns tag which is selected in snapshot data tab.
     *
     * If any object is not selected, throws IllegalStateException.
     *
     * @return class tag which is selected.
     * @throws IllegalStateException If any object is not selected.
     */
    public long getSelectedClassTag() throws IllegalStateException {

        if (histogramTab.isSelected() && (histogramController.getSelectedData() != null)) {
            return histogramController.getSelectedData().getTag();
        } else if (objDataTable.getSelectionModel().getSelectedItem() != null) {
            return objDataTable.getSelectionModel().getSelectedItem().getTag();
        } else {
            /* This message will help user to solve this error. */
            throw new IllegalStateException("Please select Object which you want to see the reference at [SnapShot Data] Tab.");
        }

    }

    @Override
    public EventHandler<Event> getOnPluginTabSelected() {
        return null;
    }

    @Override
    public String getLicense() {
        return PluginController.LICENSE_GPL_V2;
    }

    @Override
    public Map<String, String> getLibraryLicense() {
        return null;
    }

    /**
     * Dump GC Statistics to CSV.
     *
     * @param isSelected If this value is true, this method dumps data which is
     * selected time range, otherwise this method dumps all snapshot data.
     */
    public void dumpGCStatisticsToCSV(boolean isSelected) {
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Select CSV files");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("CSV file (*.csv)", "*.csv"),
                new ExtensionFilter("All files", "*.*"));
        File csvFile = dialog.showSaveDialog(WindowController.getInstance().getOwner());

        if (csvFile != null) {
            TaskAdapter<CSVDumpGC> task = new TaskAdapter<>(new CSVDumpGC(csvFile, isSelected ? currentTarget.get() : startCombo.getItems()));
            super.bindTask(task);

            Thread parseThread = new Thread(task);
            parseThread.start();
        }

    }

    /**
     * Dump Java Class Histogram to CSV.
     *
     * @param isSelected If this value is true, this method dumps data which is
     * selected in class filter, otherwise this method dumps all snapshot data.
     */
    public void dumpClassHistogramToCSV(boolean isSelected) {
        FileChooser dialog = new FileChooser();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));

        dialog.setTitle(resource.getString("dialog.csvchooser.title"));
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("CSV file (*.csv)", "*.csv"),
                new ExtensionFilter("All files", "*.*"));
        File csvFile = dialog.showSaveDialog(WindowController.getInstance().getOwner());

        if (csvFile != null) {
            Predicate<? super ObjectData> filter = histogramController.getFilter();
            TaskAdapter<CSVDumpHeap> task = new TaskAdapter<>(new CSVDumpHeap(csvFile, isSelected ? currentTarget.get() : startCombo.getItems(), isSelected ? filter : null, HeapStatsUtils.getReplaceClassName()));
            super.bindTask(task);

            Thread parseThread = new Thread(task);
            parseThread.start();
        }

    }

    @Override
    public Runnable getOnCloseRequest() {
        return null;
    }

    @Override
    public void setData(Object data, boolean select) {
        super.setData(data, select);
        snapshotList.setText((String) data);

        TaskAdapter<ParseHeader> task = new TaskAdapter<>(new ParseHeader(Arrays.asList((String) data), HeapStatsUtils.getReplaceClassName(), true));
        task.setOnSucceeded(evt -> {
            startCombo.setItems(FXCollections.observableArrayList(task.getTask().getSnapShotList()));
            endCombo.setItems(FXCollections.observableArrayList(task.getTask().getSnapShotList()));
            startCombo.getSelectionModel().selectFirst();
            endCombo.getSelectionModel().selectLast();
        });
        super.bindTask(task);

        Thread parseThread = new Thread(task);
        parseThread.start();
    }

    private void drawRebootSuspectLine(XYChart<String, ? extends Number> target) {

        if (target.getData().isEmpty() || target.getData().get(0).getData().isEmpty()) {
            return;
        }

        AnchorPane anchor = (AnchorPane) target.getParent().getChildrenUnmodifiable()
                .stream()
                .filter(n -> n instanceof AnchorPane)
                .findAny()
                .get();
        ObservableList<Node> anchorChildren = anchor.getChildren();
        anchorChildren.clear();

        CategoryAxis xAxis = (CategoryAxis) target.getXAxis();
        Axis yAxis = target.getYAxis();
        Label chartTitle = (Label) target.getChildrenUnmodifiable().stream()
                .filter(n -> n.getStyleClass().contains("chart-title"))
                .findFirst()
                .get();

        double startX = xAxis.getLayoutX() + xAxis.getStartMargin() - 1.0d;
        double yPos = yAxis.getLayoutY() + chartTitle.getLayoutY() + chartTitle.getHeight();
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        List<Rectangle> rectList = summaryData.get().getRebootSuspectList()
                .stream()
                .map(d -> converter.toString(d))
                .map(s -> new Rectangle(xAxis.getDisplayPosition(s) + startX, yPos, 4d, yAxis.getHeight()))
                .peek(r -> ((Rectangle) r).setStyle("-fx-fill: yellow;"))
                .collect(Collectors.toList());
        anchorChildren.addAll(rectList);
    }

}
