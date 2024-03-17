package uni.fmi.imagedownsizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class ImageDownsizer extends Application {

    @FXML
    private Label uploadFileLabel;
    @FXML
    private Button uploadFileButton;
    @FXML
    private Slider downsizeSlider;
    @FXML
    private Label downsizeLabel;
    @FXML
    private CheckBox parallelCheckbox;
    @FXML
    private Button downsizeButton;
    @FXML
    private Label resultLabel;

    private File selectedFile;

    @FXML
    public void initialize() {
        uploadFileButton.setOnAction(this::handleUploadFile);
        downsizeButton.setOnAction(this::handleDownsize);
        downsizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                downsizeLabel.setText("Downsize Factor: " + Math.round(newValue.doubleValue() * 100) + "% of the original size");
            }
        });
    }

    private void handleDownsize(ActionEvent actionEvent) {
        if (selectedFile == null) {
            uploadFileLabel.setText("!! Please, upload an image first !!");
            return;
        }

        double downsizeFactor = downsizeSlider.valueProperty().getValue().doubleValue();

        Downsizer downsizer = new Downsizer(downsizeFactor, selectedFile);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Instant before = Instant.now();

                try {
                    if (!parallelCheckbox.isSelected())
                        downsizer.consequentialDownsize();
                    else
                        downsizer.parallelDownsize();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Instant after = Instant.now();

                Duration benchmark = Duration.between(before, after);
                Platform.runLater(() -> {
                    resultLabel.setText(resultLabel.getText() + "\n" + "## Downsizing successful!\nYou can find the result in the same directory as the original image." + "\nBenchmark: " + benchmark.getSeconds() + " seconds\n");
                });

                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleUploadFile(ActionEvent actionEvent) {
        Stage stage = (Stage) uploadFileButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an image");

        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            String name = selectedFile.getName();
            int extensionIndex = name.lastIndexOf('.');
            String extension = name.substring(extensionIndex + 1);

            if (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")) {
                uploadFileLabel.setText("!! Please upload a pgn, jpg or jpeg file !!");
                selectedFile = null;
                return;
            }

            uploadFileLabel.setText("Selected image: " + selectedFile.getAbsolutePath());
        } else {
            uploadFileLabel.setText("No image selected.");
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageDownsizer.class.getResource("imageDownsizer.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 450, 650);
        stage.setTitle("ImageDownsizer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}