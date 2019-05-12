package Comp;

import java.awt.*;

import javax.imageio.ImageReader;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.opencv.core.*;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;


public class MainUi extends JFrame {

    private JPanel content;
    private JTextField dirText;
    private String checkDir = "";
    private String[] methodTextList = { "Histogram Comparison(FAST) ", "Template Matching", "FeatureMatching(Slow)", "COMPLEX(Very Slow)"};
    private Comparator c = new Comparator();
    private JProgressBar progressBar;
    private JLabel elapsedTime;
    private ArrayList<ImageInfo> resultImageList;

    private boolean changed = false;

    public static void main(String[] args) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);



        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    try {
                        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    } catch (Exception e) {
                    }
                    MainUi frame = new MainUi();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MainUi() {
        setResizable(false);
        setTitle("Image Comparator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1015, 596);

        resultImageList = new ArrayList<>();

        content = new JPanel();
        content.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(content);
        content.setLayout(new BorderLayout(0, 0));

        JPanel option = new JPanel();
        content.add(option, BorderLayout.WEST);
        option.setLayout(new MigLayout("", "[]", "[33px][33.00][][][][][]"));

        JPanel directoryPanel = new JPanel();
        option.add(directoryPanel, "cell 0 0,growx,aligny top");

        JLabel dirLabel = new JLabel("Directory");
        directoryPanel.add(dirLabel);

        dirText = new JTextField();
        directoryPanel.add(dirText);
        dirText.setColumns(15);

        JButton dirBtn = new JButton("...");
        dirBtn.addActionListener((ActionEvent e)-> {
            JFileChooser fileDialog = new JFileChooser();

            if(!checkDir.equals("")) fileDialog.setCurrentDirectory(new File(checkDir));

            fileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returnValue = fileDialog.showOpenDialog(null);

            if(returnValue == JFileChooser.APPROVE_OPTION){
                checkDir = fileDialog.getSelectedFile().getAbsolutePath();
                dirText.setText(checkDir);
                option.updateUI();
                changed = true;
            }
        });

        directoryPanel.add(dirBtn);

        JPanel methodPanel = new JPanel();
        option.add(methodPanel, "cell 0 1,growx,aligny top");

        JLabel methodLabel = new JLabel("Comparison Method");
        methodPanel.add(methodLabel);

        JComboBox methodCombo = new JComboBox(methodTextList);
        methodPanel.add(methodCombo);

        JPanel comparisonPanel = new JPanel();
        option.add(comparisonPanel, "cell 0 2,growx,aligny top");

        JLabel levelLabel = new JLabel("Comparison Level");
        comparisonPanel.add(levelLabel);

        JSlider levelSlider = new JSlider();
        option.add(levelSlider, "cell 0 3,growx,aligny top");
        levelSlider.setSnapToTicks(true);
        levelSlider.setPaintLabels(true);
        levelSlider.setValue(2);
        levelSlider.setMajorTickSpacing(1);
        levelSlider.setMinimum(1);
        levelSlider.setMaximum(3);

        JPanel resultContainer = new JPanel();
        content.add(resultContainer, BorderLayout.CENTER);
        resultContainer.setLayout(new MigLayout("", "[300px:300px:300px][320px:320px:320px]", "[][grow][]"));

        JPanel resultPanel = new JPanel();
        resultContainer.add(resultPanel, "cell 0 0 2 1,grow");

        JLabel lblResult = new JLabel("RESULT");
        resultPanel.add(lblResult);

        JScrollPane scrollPane = new JScrollPane();
        resultContainer.add(scrollPane, "cell 0 1,grow");

        JPanel scrollContent = new JPanel();

        FlowLayout flowLayout = (FlowLayout) scrollContent.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);

        scrollPane.setViewportView(scrollContent);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(8);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(null);
        resultContainer.add(buttonPanel, "cell 0 2,grow");

        JButton btnRemove = new JButton("REMOVE");
        buttonPanel.add(btnRemove);

        JButton btnMove = new JButton("MOVE");
        buttonPanel.add(btnMove);

        btnMove.addActionListener((ActionEvent e)-> {
            File f = new File(checkDir + "\\moved");

            if(!f.exists()) f.mkdirs();

            int length = resultImageList.size();
            for(int i=0; i<length; i++){
                if(resultImageList.get(i).selected){
                    File move = resultImageList.get(i).path;

                    if(move.exists()) {
                        move.renameTo(new File(f.getAbsolutePath() + "\\" + move.getName()));
                    }
                }
            }
            changed = true;
            JOptionPane.showMessageDialog(MainUi.this, f.getAbsolutePath() + " 폴더로 이미지 이동을 완료했습니다.");
        });

        btnRemove.addActionListener((ActionEvent e)-> {
            int length = resultImageList.size();
            for(int i=0; i<length; i++){
                if(resultImageList.get(i).selected){
                    File move = resultImageList.get(i).path;
                    if(move.exists()) {
                        if (move.delete()) {
                            System.out.println("Delete Complete");
                        } else {
                            System.out.println("Delete Failed..");
                        }
                    }
                }
            }
            changed = true;
            JOptionPane.showMessageDialog(MainUi.this, "이미지 삭제를 완료했습니다.");
        });


        progressBar = new JProgressBar();
        progressBar.setMaximum(500);

        option.add(progressBar, "cell 0 5,growx");

        elapsedTime = new JLabel("");
        option.add(elapsedTime, "cell 0 6,alignx center,aligny center");

        JPanel viewer = new JPanel();
        viewer.setBorder(new LineBorder(Color.LIGHT_GRAY));
        resultContainer.add(viewer, "cell 1 1 1 2,grow");
        viewer.setLayout(new BorderLayout(0, 0));

        JPanel viewerTextPanel = new JPanel();
        viewer.add(viewerTextPanel, BorderLayout.SOUTH);
        viewerTextPanel.setLayout(new BorderLayout(0, 0));

        JLabel details = new JLabel("");
        details.setFont(new Font("나눔바른고딕", Font.PLAIN, 12));
        viewerTextPanel.add(details, BorderLayout.NORTH);

        JLabel size = new JLabel("");
        size.setFont(new Font("나눔바른고딕", Font.PLAIN, 12));
        viewerTextPanel.add(size, BorderLayout.SOUTH);

        JPanel imageViewer = new JPanel();
        viewer.add(imageViewer, BorderLayout.CENTER);

        JButton analyzeBtn = new JButton("Analyze");
        analyzeBtn.addActionListener((ActionEvent e)-> {
            resultImageList.clear();
            imageViewer.removeAll();
            details.setText("");
            size.setText("");

            String path = checkDir;
            analyzeBtn.setEnabled(false);
            btnMove.setEnabled(false);
            btnRemove.setEnabled(false);
            //
            int methodValue = methodCombo.getSelectedIndex();
            int criteria = levelSlider.getValue();

            if(path == null){
                JOptionPane.showMessageDialog(MainUi.this, "경로를 설정해주세요.");
            } else {
                File objectDir = new File(path);
                File[] fileList = objectDir.listFiles((File dir, String name)->{
                    String uName = name.toUpperCase();
                    return uName.endsWith("PNG") || uName.endsWith("JPG") || uName.endsWith("JPEG");// || uName.endsWith("GIF");
                });

                new Thread(()->{
                        long startTime = System.currentTimeMillis();

                        ArrayList<ArrayList<File>> group = c.calculate(fileList, criteria, methodValue);

                        if(group == null){
                            JOptionPane.showMessageDialog(MainUi.this, "예기치 못한 오류가 발생했습니다. 다시 실행해주세요.");
                            return;
                        }

                        scrollContent.removeAll();

                        JPanel p = new JPanel();
                        p.setLayout(new GridLayout(0,1));

                        double currentProgress = progressBar.getValue();
                        double increment = 100.0 / group.size();

                        for(ArrayList<File> list : group) {
                            currentProgress += increment;
                            progressBar.setValue((int)currentProgress);
                            if(list.size() == 1) continue;

                            JPanel temp = new JPanel();
                            temp.setLayout(new FlowLayout(FlowLayout.LEFT, 5,5));

                            for(File f : list) {
                                try {

                                    Image i = ImageIO.read(f);
                                    int width = ((BufferedImage) i).getWidth();
                                    int height = ((BufferedImage) i).getHeight();

                                    JLabel image = new JLabel(new ImageIcon(i.getScaledInstance(70,70,Image.SCALE_FAST)));
                                    resultImageList.add(new ImageInfo(image, f, width, height,i.getScaledInstance(300,300,Image.SCALE_FAST)));

                                    image.addMouseListener(new MouseListener() {
                                        @Override
                                        public void mouseClicked(MouseEvent e) {
                                            JLabel s = (JLabel)e.getSource();
                                            int length = resultImageList.size();

                                            for(int i=0; i<length; i++){
                                                if(s.equals(resultImageList.get(i).label)){
                                                    if(s.isEnabled()) {
                                                        resultImageList.get(i).selected = true;
                                                        s.setEnabled(false);
                                                    } else {
                                                        resultImageList.get(i).selected = false;
                                                        s.setEnabled(true);
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        @Override
                                        public void mousePressed(MouseEvent e) {}
                                        @Override
                                        public void mouseReleased(MouseEvent e) {}
                                        @Override
                                        public void mouseEntered(MouseEvent e) {
                                            JLabel s = (JLabel)e.getSource();
                                            int length = resultImageList.size();

                                            for(int i=0; i<length; i++){
                                                if(s.equals(resultImageList.get(i).label)){
                                                    ImageInfo my = resultImageList.get(i);

                                                    try {
                                                        imageViewer.add(new JLabel(new ImageIcon(resultImageList.get(i).scaledImage)));
                                                        details.setText(resultImageList.get(i).path.getAbsolutePath());
                                                        size.setText(resultImageList.get(i).getDetails());
                                                    }catch(Exception err){
                                                        err.printStackTrace();
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        @Override
                                        public void mouseExited(MouseEvent e) {
                                            imageViewer.removeAll();
                                            details.setText("");
                                            size.setText("");
                                        }
                                    });


                                    temp.add(image);
                                }catch(Exception err){
                                    err.printStackTrace();
                                }
                            }
                            p.add(temp);
                        }

                        long endTime = System.currentTimeMillis();

                        progressBar.setValue(progressBar.getMaximum());

                        analyzeBtn.setEnabled(true);
                        btnMove.setEnabled(true);
                        btnRemove.setEnabled(true);

                        scrollContent.add(p);
                        scrollPane.updateUI();

                        elapsedTime.setText("estimated time: " + (endTime-startTime)/1000.0 + "s");
                        progressBar.setValue(0);

                        changed = false;
                }).start();
            }
        });
        option.add(analyzeBtn, "cell 0 4,grow");
    }
    private class ImageInfo {
        JLabel label;
        File path;
        int width;
        int height;
        boolean selected;
        Image scaledImage;

        ImageInfo(JLabel label, File path, int width, int height, Image image){
            this.label = label;
            this.path = path;
            this.width = width;
            this.height = height;
            this.selected = false;
            this.scaledImage = image;
        }

        public String getDetails(){
            String s = "";
            String absPath = path.getAbsolutePath().toUpperCase();

            if(absPath.endsWith("PNG")) s = "PNG";
            else if(absPath.endsWith("JPG") || absPath.endsWith("JPEG")) s = "JPG";
            else if(absPath.endsWith("GIF") || absPath.endsWith("gif")) s= "GIF";

            return this.width + "X" + this.height + " " + s;
        }

    }

    public class Comparator {
        private File[] file;
        private String[] fileList;
        private Mat[] destMat;
        private int criteria;

        public ArrayList<ArrayList<File>> calculate(File[] fileList, int criteria, int method){

            this.criteria = criteria;
            this.file = fileList;
            this.fileList = new String[fileList.length];

            for(int i=0; i<fileList.length; i++)
                this.fileList[i] = fileList[i].getAbsolutePath();

            if(method == 0) return calcHistogram();
            else if(method == 1) return templateMatching();
            else if(method == 2) return featureComparison();
            else if(method == 3) return null;

            return null;
        }

        public ArrayList<ArrayList<File>> calcHistogram(){
            long startTime = System.currentTimeMillis();

            int length = fileList.length;

            double currentProgress = 0;
            double increment = 100.0 / length;

            // 유사한 이미지들을 그룹핑하기 위해서 배열선언
            boolean[] check = new boolean[length];
            for(int i=0; i<length; i++) check[i] = false;

            // 그룹 번호는 0부터 시작.
            int groupNum = 0;

            Mat[] imgMat;

            // 이미지 읽어들이기. 히스토그램 분석한 결과를 저장해 놓을 배열 설정.
            // 경로 또는 이동, 삭제 동작이 수행되지 않은 경우
            // 메모리에 저장된 destMat 을 이용해서 연산을 수행한다.
            if(changed) {
                imgMat = new Mat[length];
                destMat = new Mat[length];

                for (int i = 0; i < length; i++) {
                    currentProgress += increment;
                    progressBar.setValue((int) currentProgress);

                    imgMat[i] = new Mat();
                    try {
                        imgMat[i] = Imgcodecs.imread(fileList[i], Imgcodecs.CV_LOAD_IMAGE_COLOR);
                    } catch (Exception e) {
                        System.out.println(fileList[i]);
                        e.printStackTrace();
                    }

                }

                // 비교할 때 명도값이 필요없기 때문에 HSV모델로 변경.
                for (int i = 0; i < length; i++) {
                    currentProgress += increment;
                    progressBar.setValue((int) currentProgress);

                    destMat[i] = new Mat();
                    try {
                        Imgproc.cvtColor(imgMat[i], destMat[i], Imgproc.COLOR_BGR2HSV);
                    } catch (Exception e) {
                        System.out.println(fileList[i]);
                        e.printStackTrace();
                    }
                }


                // 히스토그램 계산을 위한 변수들 선언
                int hBins = 50, sBins = 60;
                int[] histSize = {hBins, sBins};
                float[] ranges = {0, 180, 0, 256};
                int[] channels = {0, 1};

                // 각 결과값을 destMat 에 저장..
                for (int i = 0; i < length; i++) {
                    currentProgress += increment;
                    progressBar.setValue((int) currentProgress);

                    List<Mat> baseList = Arrays.asList(destMat[i]);
                    Imgproc.calcHist(baseList, new MatOfInt(channels), new Mat(), destMat[i], new MatOfInt(histSize), new MatOfFloat(ranges), false);
                    Core.normalize(destMat[i], destMat[i], 0, 1, Core.NORM_MINMAX);
                }
            } else progressBar.setValue(300);

            // 실제 비교하는 부분.
            double compareValue[] = {0.1, 10.0, 8.0};
            ArrayList<ArrayList<File>> group = new ArrayList<>();

            // i: 현재 기준이 될 이미지
            // j: i번째 이미지와 비교할 이미지들..
            for(int i =0;i<length; i++){
                progressBar.setValue((int)currentProgress);
                currentProgress += increment;

                if(check[i]) continue; // 그룹핑 된 이미지는 또 계산하지 않는다.

                ArrayList<File> temp = new ArrayList<>();
                temp.add(file[i]);

                for(int j=i+1; j<length; j++){
                    if(check[j]) continue; // 그룹핑 된 이미지는 또 계산하지 않는다.

                    boolean isSame = true;

                    for(int method = 0; method < criteria && isSame; method++) {
                        double result = Imgproc.compareHist(destMat[i], destMat[j], method);
                        switch(method){
                            case 0:
                                if(result < 1- compareValue[method]) isSame = false;
                                break;
                            case 1:
                                if(result > compareValue[method]) isSame = false;
                                break;
                            case 2:
                                if(result < compareValue[method]) isSame = false;
                                break;
                            default:
                                if(result < 1- compareValue[0]) isSame = false;
                                break;
                        }
                    }
                    if(isSame){
                        temp.add(file[j]);
                        check[j] = true;
                    }
                }
                group.add(temp);
            }

            System.out.println("estimated Time: " + (System.currentTimeMillis()-startTime)/1000.0 + "s");

            return group;
        }
        public ArrayList<ArrayList<File>> featureComparison(){
            long startTime = System.currentTimeMillis();

            int length = fileList.length;

            double currentProgress = 0;
            double increment = 200.0 / length;

            boolean[] check = new boolean[length];
            for(int i=0; i<length; i++) check[i] = false;

            Mat[] img = new Mat[length];
            Mat[] descriptors = new Mat[length];
            MatOfKeyPoint[] keyPoints = new MatOfKeyPoint[length];

            FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
            DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            MatOfDMatch matches = new MatOfDMatch();

            for(int i=0; i<length; i++){
                currentProgress += increment;
                progressBar.setValue((int)currentProgress);

                img[i] = new Mat();
                descriptors[i] = new Mat();
                keyPoints[i] = new MatOfKeyPoint();

                try {
                    img[i] = Imgcodecs.imread(fileList[i], Imgcodecs.CV_LOAD_IMAGE_COLOR);
                }catch(Exception e){
                    e.printStackTrace();
                }

                detector.detect(img[i], keyPoints[i]);
                extractor.compute(img[i], keyPoints[i], descriptors[i]);
            }

            int compareValue = 5 + criteria;
            ArrayList<ArrayList<File>> group = new ArrayList<>();

            for(int i =0;i<length; i++){
                currentProgress += increment;
                progressBar.setValue((int)currentProgress);

                if(check[i]) continue; // 그룹핑 된 이미지는 또 계산하지 않는다.

                ArrayList<File> temp = new ArrayList<>();
                temp.add(file[i]);

                for(int j=i+1; j<length; j++){
                    int ret = 0;

                    if (descriptors[j].cols() == descriptors[i].cols()) {
                        matcher.match(descriptors[i], descriptors[j] ,matches);

                        // Check matches of key points
                        DMatch[] match = matches.toArray();
                        double max_dist = 0; double min_dist = 100;

                        for (int k = 0; k < descriptors[i].rows(); k++) {
                            double dist = match[k].distance;
                            if( dist < min_dist ) min_dist = dist;
                            if( dist > max_dist ) max_dist = dist;
                        }

                        // Extract good images (distances are under 10)
                        for (int k = 0; k < descriptors[i].rows(); k++) {
                            if (match[k].distance <= 10) {
                                ret++;
                            }
                        }
                    }

                    if(ret >= compareValue){
                        temp.add(file[j]);
                        check[j] = true;
                    }
                }
                group.add(temp);
            }

            System.out.println("estimated time: " +(System.currentTimeMillis() - startTime)/1000.0 + "s");
            return group;
        }
        public ArrayList<ArrayList<File>> templateMatching(){

            return null;
        }
    }
}
