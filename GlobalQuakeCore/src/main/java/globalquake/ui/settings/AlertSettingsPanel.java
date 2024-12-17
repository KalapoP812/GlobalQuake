package globalquake.ui.settings;

import globalquake.core.Settings;
import globalquake.core.earthquake.data.Cluster;

import javax.swing.*;
import java.awt.*;
import java.util.stream.IntStream;

public class AlertSettingsPanel extends SettingsPanel {

    private JCheckBox chkBoxLocal;
    private JTextField textFieldLocalDist;
    private JCheckBox chkBoxRegion;
    private JTextField textFieldRegionMag;
    private JTextField textFieldRegionDist;
    private JCheckBox checkBoxGlobal;
    private JTextField textFieldGlobalMag;
    private JLabel label1;
    private JCheckBox chkBoxFocus;
    private JCheckBox chkBoxJumpToAlert;
    private IntensityScaleSelector shakingThreshold;
    private IntensityScaleSelector strongShakingThreshold;
    private JCheckBox chkBoxPossibleShaking;
    private JTextField textFieldPossibleShakingDistance;
    private JCheckBox chkBoxEarthquakeSounds;
    private JTextField textFieldQuakeMinMag;
    private JTextField textFieldQuakeMaxDist;
    private JLabel label2;
    private IntensityScaleSelector eewThreshold;
    private JComboBox<Integer> comboBoxEEWClusterLevel;
    private JTextField textFieldNtfy;
    private JTextField textFieldPushoverUserID;
    private JTextField textFieldPushoverToken;
    private JCheckBox chkBoxNtfy;
    private JCheckBox chkBoxPushover;
    private JCheckBox chkBoxPushoverCustomSounds;
    private JTextField textFieldPushoverSoundDetected;
    private JTextField textFieldPushoverSoundFeltLight;
    private JTextField textFieldPushoverSoundFeltStrong;
    private JCheckBox chkBoxNtfyNearbyShaking;
    private JCheckBox chkBoxNtfyFeltShaking;
    private JComboBox<Integer> ntfyNearbyShakingPriorityListJComboBox;
    private JComboBox<Integer> ntfyLightShakingPriorityListJComboBox;
    private JComboBox<Integer> ntfyStrongShakingPriorityListJComboBox;
    private JCheckBox chkBoxPushoverNearbyShaking;
    private JCheckBox chkBoxPushoverFeltShaking;
    private JComboBox<Integer> pushoverNearbyShakingPriorityListJComboBox;
    private JComboBox<Integer> pushoverLightShakingPriorityListJComboBox;
    private JComboBox<Integer> pushoverStrongShakingPriorityListJComboBox;
    private JCheckBox chkBoxNtfySendRevisions;
    private JCheckBox chkBoxPushoverSendRevisions;


    public AlertSettingsPanel() {
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Warnings", createWarningsTab());
        tabbedPane.addTab("Pings", createPingsTab());
        tabbedPane.addTab("Ntfy", createNtfyTab());
        tabbedPane.addTab("Pushover", createPushoverTab());

        add(tabbedPane, BorderLayout.CENTER);

        refreshUI();
    }

    private Component createPingsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        createPossibleShakingPanel(panel);
        createEarthquakeSoundsPanel(panel);

        JPanel eewThresholdPanel = new JPanel(new GridLayout(3, 1));
        eewThresholdPanel.setBorder(BorderFactory.createTitledBorder("EEW"));
        eewThresholdPanel.add(new JLabel("Trigger eew_warning.wav sound effect if estimated intensity at land reaches: "));

        eewThresholdPanel.add(eewThreshold = new IntensityScaleSelector("",
                Settings.eewScale, Settings.eewLevelIndex));


        JPanel maxClusterLevelPanel = new JPanel();
        maxClusterLevelPanel.add(new JLabel("and the associated Cluster has level at least: "));

        comboBoxEEWClusterLevel = new JComboBox<>();
        for (int i : IntStream.rangeClosed(0, Cluster.MAX_LEVEL).toArray()) {
            comboBoxEEWClusterLevel.addItem(i);
        }

        comboBoxEEWClusterLevel.setSelectedIndex(Settings.eewClusterLevel);

        maxClusterLevelPanel.add(comboBoxEEWClusterLevel);

        eewThresholdPanel.add(maxClusterLevelPanel);

        panel.add(eewThresholdPanel);

        fill(panel, 20);

        return panel;
    }

    private void createEarthquakeSoundsPanel(JPanel panel) {
        chkBoxEarthquakeSounds = new JCheckBox("Play sound alerts if earthquake is bigger than (magnitude): ", Settings.enableEarthquakeSounds);
        textFieldQuakeMinMag = new JTextField(String.valueOf(Settings.earthquakeSoundsMinMagnitude), 12);
        textFieldQuakeMinMag.setEnabled(chkBoxEarthquakeSounds.isSelected());
        textFieldQuakeMaxDist = new JTextField("1", 12);
        textFieldQuakeMaxDist.setEnabled(chkBoxEarthquakeSounds.isSelected());

        chkBoxEarthquakeSounds.addChangeListener(changeEvent -> {
            textFieldQuakeMinMag.setEnabled(chkBoxEarthquakeSounds.isSelected());
            textFieldQuakeMaxDist.setEnabled(chkBoxEarthquakeSounds.isSelected());
        });

        JPanel earthquakePanel = new JPanel(new GridLayout(2, 1));
        earthquakePanel.setBorder(BorderFactory.createTitledBorder("Earthquake alerts"));

        JPanel quakeMagpanel = new JPanel();
        quakeMagpanel.setLayout(new BoxLayout(quakeMagpanel, BoxLayout.X_AXIS));
        quakeMagpanel.add(chkBoxEarthquakeSounds);
        quakeMagpanel.add(textFieldQuakeMinMag);

        earthquakePanel.add(quakeMagpanel);

        JPanel quakeDistPanel = new JPanel();
        quakeDistPanel.setLayout(new BoxLayout(quakeDistPanel, BoxLayout.X_AXIS));
        quakeDistPanel.add(label2 = new JLabel(""));
        quakeDistPanel.add(textFieldQuakeMaxDist);

        earthquakePanel.add(quakeDistPanel);

        panel.add(earthquakePanel);
    }

    private void createPossibleShakingPanel(JPanel panel) {
        chkBoxPossibleShaking = new JCheckBox("Play sound if possible shaking is detected closer than (%s): ".formatted(Settings.getSelectedDistanceUnit().getShortName()), Settings.alertPossibleShaking);
        textFieldPossibleShakingDistance = new JTextField(String.valueOf(Settings.alertPossibleShakingDistance), 12);
        textFieldPossibleShakingDistance.setEnabled(chkBoxPossibleShaking.isSelected());


        chkBoxPossibleShaking.addChangeListener(changeEvent -> textFieldPossibleShakingDistance.setEnabled(chkBoxPossibleShaking.isSelected()));

        JPanel possibleShakingPanel = new JPanel(new GridLayout(1, 1));
        possibleShakingPanel.setBorder(BorderFactory.createTitledBorder("Possible shaking detection"));

        JPanel regionMagPanel = new JPanel();
        regionMagPanel.setLayout(new BoxLayout(regionMagPanel, BoxLayout.X_AXIS));
        regionMagPanel.add(chkBoxPossibleShaking);
        regionMagPanel.add(textFieldPossibleShakingDistance);

        possibleShakingPanel.add(regionMagPanel);

        panel.add(possibleShakingPanel);
    }

    private Component createWarningsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createAlertDialogSettings());
        panel.add(createAlertLevels());

        fill(panel, 10);

        return panel;
    }

    private Component createAlertLevels() {
        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Alert levels"));

        panel.add(shakingThreshold = new IntensityScaleSelector("Shaking alert threshold: ",
                Settings.shakingLevelScale, Settings.shakingLevelIndex));
        panel.add(strongShakingThreshold = new IntensityScaleSelector("Strong shaking alert threshold: ",
                Settings.strongShakingLevelScale, Settings.strongShakingLevelIndex));

        return panel;
    }

    private Component createAlertDialogSettings() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Alert settings"));

        chkBoxLocal = new JCheckBox("", Settings.alertLocal);
        textFieldLocalDist = new JTextField("1", 12);
        textFieldLocalDist.setEnabled(chkBoxLocal.isSelected());
        chkBoxLocal.addChangeListener(changeEvent -> textFieldLocalDist.setEnabled(chkBoxLocal.isSelected()));

        JPanel localPanel = new JPanel(new GridLayout(1, 1));
        localPanel.setBorder(BorderFactory.createTitledBorder("Local area"));

        JPanel nearbyPanel = new JPanel();
        nearbyPanel.setLayout(new BoxLayout(nearbyPanel, BoxLayout.X_AXIS));
        nearbyPanel.add(chkBoxLocal);
        nearbyPanel.add(textFieldLocalDist);

        localPanel.add(nearbyPanel);
        panel.add(localPanel);

        chkBoxRegion = new JCheckBox("Alert earthquakes larger than (magnitude): ", Settings.alertRegion);
        textFieldRegionMag = new JTextField(String.valueOf(Settings.alertRegionMag), 12);
        textFieldRegionMag.setEnabled(chkBoxRegion.isSelected());
        textFieldRegionDist = new JTextField("1", 12);
        textFieldRegionDist.setEnabled(chkBoxRegion.isSelected());

        chkBoxRegion.addChangeListener(changeEvent -> {
            textFieldRegionMag.setEnabled(chkBoxRegion.isSelected());
            textFieldRegionDist.setEnabled(chkBoxRegion.isSelected());
        });

        JPanel regionPanel = new JPanel(new GridLayout(2, 1));
        regionPanel.setBorder(BorderFactory.createTitledBorder("Regional area"));

        JPanel regionMagPanel = new JPanel();
        regionMagPanel.setLayout(new BoxLayout(regionMagPanel, BoxLayout.X_AXIS));
        regionMagPanel.add(chkBoxRegion);
        regionMagPanel.add(textFieldRegionMag);

        regionPanel.add(regionMagPanel);


        JPanel regionDistPanel = new JPanel();
        regionDistPanel.setLayout(new BoxLayout(regionDistPanel, BoxLayout.X_AXIS));
        regionDistPanel.add(label1 = new JLabel(""));
        regionDistPanel.add(textFieldRegionDist);

        regionPanel.add(regionDistPanel);

        panel.add(regionPanel);

        JPanel globalPanel = new JPanel(new GridLayout(1, 1));
        globalPanel.setBorder(BorderFactory.createTitledBorder("Global"));

        checkBoxGlobal = new JCheckBox("Alert earthquakes larger than (magnitude): ", Settings.alertGlobal);
        textFieldGlobalMag = new JTextField(String.valueOf(Settings.alertGlobalMag), 12);
        textFieldGlobalMag.setEnabled(checkBoxGlobal.isSelected());
        checkBoxGlobal.addChangeListener(changeEvent -> textFieldGlobalMag.setEnabled(checkBoxGlobal.isSelected()));

        JPanel globalMagPanel = new JPanel();
        globalMagPanel.setLayout(new BoxLayout(globalMagPanel, BoxLayout.X_AXIS));

        globalMagPanel.add(checkBoxGlobal);
        globalMagPanel.add(textFieldGlobalMag);

        globalPanel.add(globalMagPanel);

        panel.add(globalPanel);

        JPanel panel2 = new JPanel(new GridLayout(2, 1));

        panel2.add(chkBoxFocus = new JCheckBox("Focus main window if the conditions above are met", Settings.focusOnEvent));
        panel2.add(chkBoxJumpToAlert = new JCheckBox("Jump directly to the warned event", Settings.jumpToAlert));

        panel.add(panel2);

        return panel;
    }

    private Component createNtfyTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createNtfySettings());

        fill(panel, 10);

        return panel;
    }

    private Component createNtfySettings() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Ntfy Settings"));

        chkBoxNtfy = new JCheckBox("Use Ntfy", Settings.useNtfy);
        textFieldNtfy = new JTextField(Settings.ntfy, 12);
        textFieldNtfy.setEnabled(chkBoxNtfy.isSelected());
        chkBoxNtfy.addChangeListener(changeEvent -> textFieldNtfy.setEnabled(chkBoxNtfy.isSelected()));

        JPanel ntfyPanel = new JPanel(new GridLayout(3, 1));
        ntfyPanel.setBorder(BorderFactory.createTitledBorder("Ntfy Url"));

        JPanel ntfyUseNtfyPanel = new JPanel();
        ntfyUseNtfyPanel.setLayout(new BoxLayout(ntfyUseNtfyPanel, BoxLayout.X_AXIS));
        ntfyUseNtfyPanel.add(chkBoxNtfy);

        ntfyPanel.add(ntfyUseNtfyPanel);

        JPanel ntfyUrlPanel = new JPanel();
        ntfyUrlPanel.setLayout(new BoxLayout(ntfyUrlPanel, BoxLayout.X_AXIS));
        ntfyUrlPanel.add(new JLabel("Ntfy Url: "));
        ntfyUrlPanel.add(textFieldNtfy);

        ntfyPanel.add(ntfyUrlPanel);

        JPanel ntfyUrlMessagePanel = new JPanel();
        ntfyUrlMessagePanel.setLayout(new BoxLayout(ntfyUrlMessagePanel, BoxLayout.X_AXIS));
        ntfyUrlMessagePanel.add(new JLabel("For multiple urls, separate them with commas"));

        ntfyPanel.add(ntfyUrlMessagePanel);

        panel.add(ntfyPanel);

        chkBoxNtfyNearbyShaking = new JCheckBox("Notify when nearby shaking is detected", Settings.ntfyNearbyShaking);
        chkBoxNtfySendRevisions = new JCheckBox("Notify every revision update for nearby shaking", Settings.ntfySendRevisions);
        chkBoxNtfySendRevisions.setEnabled(chkBoxNtfyNearbyShaking.isSelected());
        chkBoxNtfyNearbyShaking.addChangeListener(changeEvent -> chkBoxNtfySendRevisions.setEnabled(chkBoxNtfyNearbyShaking.isSelected()));
        chkBoxNtfyFeltShaking = new JCheckBox("Notify when shaking is expected", Settings.ntfyFeltShaking);

        JPanel ntfyShakingPanel = new JPanel(new GridLayout(3, 1));
        ntfyShakingPanel.setBorder(BorderFactory.createTitledBorder("Shaking Alerts"));

        JPanel ntfyNearbyShakingPanel = new JPanel();
        ntfyNearbyShakingPanel.setLayout(new BoxLayout(ntfyNearbyShakingPanel, BoxLayout.X_AXIS));
        ntfyNearbyShakingPanel.add(chkBoxNtfyNearbyShaking);

        ntfyShakingPanel.add(ntfyNearbyShakingPanel);

        JPanel ntfySendRevisionsPanel = new JPanel();
        ntfySendRevisionsPanel.setLayout(new BoxLayout(ntfySendRevisionsPanel, BoxLayout.X_AXIS));
        ntfySendRevisionsPanel.add(chkBoxNtfySendRevisions);

        ntfyShakingPanel.add(ntfySendRevisionsPanel);

        JPanel ntfyFeltShakingPanel = new JPanel();
        ntfyFeltShakingPanel.setLayout(new BoxLayout(ntfyFeltShakingPanel, BoxLayout.X_AXIS));
        ntfyFeltShakingPanel.add(chkBoxNtfyFeltShaking);

        ntfyShakingPanel.add(ntfyFeltShakingPanel);

        panel.add(ntfyShakingPanel);

        JPanel ntfyPriorityPanel = new JPanel(new GridLayout(3, 1));
        ntfyPriorityPanel.setBorder(BorderFactory.createTitledBorder("Priority"));

        JPanel ntfyPriorityNearbyShakingPanel = new JPanel();
        ntfyPriorityNearbyShakingPanel.setLayout(new BoxLayout(ntfyPriorityNearbyShakingPanel, BoxLayout.X_AXIS));

        DefaultComboBoxModel<Integer> model1 = new DefaultComboBoxModel<>(new Integer[]{2, 3, 4, 5});
        ntfyNearbyShakingPriorityListJComboBox = new JComboBox<>(model1);
        ntfyNearbyShakingPriorityListJComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case 2: value = "Low"; break;
                        case 3: value = "Normal"; break;
                        case 4: value = "High"; break;
                        case 5: value = "Highest"; break;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        ntfyNearbyShakingPriorityListJComboBox.setSelectedItem(Settings.ntfyNearbyShakingPriorityList);

        ntfyPriorityNearbyShakingPanel.add(new JLabel("Priority for Nearby Shaking: "));
        ntfyPriorityNearbyShakingPanel.add(ntfyNearbyShakingPriorityListJComboBox);

        ntfyPriorityPanel.add(ntfyPriorityNearbyShakingPanel);

        JPanel ntfyPriorityLightShakingPanel = new JPanel();
        ntfyPriorityLightShakingPanel.setLayout(new BoxLayout(ntfyPriorityLightShakingPanel, BoxLayout.X_AXIS));

        DefaultComboBoxModel<Integer> model2 = new DefaultComboBoxModel<>(new Integer[]{2, 3, 4, 5});
        ntfyLightShakingPriorityListJComboBox = new JComboBox<>(model2);
        ntfyLightShakingPriorityListJComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case 2: value = "Low"; break;
                        case 3: value = "Normal"; break;
                        case 4: value = "High"; break;
                        case 5: value = "Highest"; break;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        ntfyLightShakingPriorityListJComboBox.setSelectedItem(Settings.ntfyLightShakingPriorityList);

        ntfyPriorityLightShakingPanel.add(new JLabel("Priority for Light Shaking: "));
        ntfyPriorityLightShakingPanel.add(ntfyLightShakingPriorityListJComboBox);

        ntfyPriorityPanel.add(ntfyPriorityLightShakingPanel);

        JPanel ntfyPriorityStrongShakingPanel = new JPanel();
        ntfyPriorityStrongShakingPanel.setLayout(new BoxLayout(ntfyPriorityStrongShakingPanel, BoxLayout.X_AXIS));

        DefaultComboBoxModel<Integer> model3 = new DefaultComboBoxModel<>(new Integer[]{2, 3, 4, 5});
        ntfyStrongShakingPriorityListJComboBox = new JComboBox<>(model3);
        ntfyStrongShakingPriorityListJComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case 2: value = "Low"; break;
                        case 3: value = "Normal"; break;
                        case 4: value = "High"; break;
                        case 5: value = "Highest"; break;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        ntfyStrongShakingPriorityListJComboBox.setSelectedItem(Settings.ntfyStrongShakingPriorityList);

        ntfyPriorityStrongShakingPanel.add(new JLabel("Priority for Strong Shaking: "));
        ntfyPriorityStrongShakingPanel.add(ntfyStrongShakingPriorityListJComboBox);

        ntfyPriorityPanel.add(ntfyPriorityStrongShakingPanel);

        panel.add(ntfyPriorityPanel);

        return panel;
    }

    private Component createPushoverTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createPushoverSettings());

        fill(panel, 10);

        return panel;
    }

    private Component createPushoverSettings() {
        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Pushover Settings"));

        chkBoxPushover = new JCheckBox("Use Pushover", Settings.usePushover);
        textFieldPushoverUserID = new JTextField(Settings.pushoverUserID, 12);
        textFieldPushoverUserID.setEnabled(chkBoxPushover.isSelected());
        textFieldPushoverToken = new JTextField(Settings.pushoverToken, 12);
        textFieldPushoverToken.setEnabled(chkBoxPushover.isSelected());
        chkBoxPushover.addChangeListener(changeEvent -> {
            textFieldPushoverUserID.setEnabled(chkBoxPushover.isSelected());
            textFieldPushoverToken.setEnabled(chkBoxPushover.isSelected());
        });

        JPanel pushoverPanel = new JPanel(new GridLayout(4, 1));
        pushoverPanel.setBorder(BorderFactory.createTitledBorder("Pushover Credentials"));

        JPanel usePushoverPanel = new JPanel();
        usePushoverPanel.setLayout(new BoxLayout(usePushoverPanel, BoxLayout.X_AXIS));
        usePushoverPanel.add(chkBoxPushover);

        pushoverPanel.add(usePushoverPanel);

        JPanel pushoverIDPanel = new JPanel();
        pushoverIDPanel.setLayout(new BoxLayout(pushoverIDPanel, BoxLayout.X_AXIS));
        pushoverIDPanel.add(new JLabel("User ID: "));
        pushoverIDPanel.add(textFieldPushoverUserID);

        pushoverPanel.add(pushoverIDPanel);

        JPanel pushoverTokenPanel = new JPanel();
        pushoverTokenPanel.setLayout(new BoxLayout(pushoverTokenPanel, BoxLayout.X_AXIS));
        pushoverTokenPanel.add(new JLabel("Token: "));
        pushoverTokenPanel.add(textFieldPushoverToken);

        pushoverPanel.add(pushoverTokenPanel);

        JPanel pushoverMessagePanel = new JPanel();
        pushoverMessagePanel.setLayout(new BoxLayout(pushoverMessagePanel, BoxLayout.X_AXIS));
        pushoverMessagePanel.add(new JLabel("For multiple users using the same token, separate them with commas"));

        pushoverPanel.add(pushoverMessagePanel);

        panel.add(pushoverPanel);

        chkBoxPushoverNearbyShaking = new JCheckBox("Notify when nearby shaking is detected", Settings.pushoverNearbyShaking);
        chkBoxPushoverSendRevisions = new JCheckBox("Notify every revision update for nearby shaking", Settings.pushoverSendRevisions);
        chkBoxPushoverSendRevisions.setEnabled(chkBoxPushoverNearbyShaking.isSelected());
        chkBoxPushoverNearbyShaking.addChangeListener(changeEvent -> chkBoxPushoverSendRevisions.setEnabled(chkBoxPushoverNearbyShaking.isSelected()));
        chkBoxPushoverFeltShaking = new JCheckBox("Notify when shaking is expected", Settings.pushoverFeltShaking);

        JPanel pushoverShakingPanel = new JPanel(new GridLayout(3, 1));
        pushoverShakingPanel.setBorder(BorderFactory.createTitledBorder("Shaking Alerts"));

        JPanel pushoverNearbyShakingPanel = new JPanel();
        pushoverNearbyShakingPanel.setLayout(new BoxLayout(pushoverNearbyShakingPanel, BoxLayout.X_AXIS));
        pushoverNearbyShakingPanel.add(chkBoxPushoverNearbyShaking);

        pushoverShakingPanel.add(pushoverNearbyShakingPanel);

        JPanel pushoverSendRevision = new JPanel();
        pushoverSendRevision.setLayout(new BoxLayout(pushoverSendRevision, BoxLayout.X_AXIS));
        pushoverSendRevision.add(chkBoxPushoverSendRevisions);

        pushoverShakingPanel.add(pushoverSendRevision);

        JPanel pushoverFeltShakingPanel = new JPanel();
        pushoverFeltShakingPanel.setLayout(new BoxLayout(pushoverFeltShakingPanel, BoxLayout.X_AXIS));
        pushoverFeltShakingPanel.add(chkBoxPushoverFeltShaking);

        pushoverShakingPanel.add(pushoverFeltShakingPanel);
        
        panel.add(pushoverShakingPanel);

        JPanel pushoverPriorityPanel = new JPanel(new GridLayout(3, 1));
        pushoverPriorityPanel.setBorder(BorderFactory.createTitledBorder("Priority"));

        JPanel pushoverPriorityNearbyShakingPanel = new JPanel();
        pushoverPriorityNearbyShakingPanel.setLayout(new BoxLayout(pushoverPriorityNearbyShakingPanel, BoxLayout.X_AXIS));

        DefaultComboBoxModel<Integer> model1 = new DefaultComboBoxModel<>(new Integer[]{-1, 0, 1});
        pushoverNearbyShakingPriorityListJComboBox = new JComboBox<>(model1);
        pushoverNearbyShakingPriorityListJComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case -1: value = "Low"; break;
                        case 0: value = "Normal"; break;
                        case 1: value = "High"; break;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        pushoverNearbyShakingPriorityListJComboBox.setSelectedItem(Settings.pushoverNearbyShakingPriorityList);

        pushoverPriorityNearbyShakingPanel.add(new JLabel("Priority for Nearby Shaking: "));
        pushoverPriorityNearbyShakingPanel.add(pushoverNearbyShakingPriorityListJComboBox);

        pushoverPriorityPanel.add(pushoverPriorityNearbyShakingPanel);

        JPanel pushoverPriorityLightShakingPanel = new JPanel();
        pushoverPriorityLightShakingPanel.setLayout(new BoxLayout(pushoverPriorityLightShakingPanel, BoxLayout.X_AXIS));

        DefaultComboBoxModel<Integer> model2 = new DefaultComboBoxModel<>(new Integer[]{-1, 0, 1});

        pushoverLightShakingPriorityListJComboBox = new JComboBox<>(model2);

        pushoverLightShakingPriorityListJComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case -1: value = "Low"; break;
                        case 0: value = "Normal"; break;
                        case 1: value = "High"; break;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        pushoverLightShakingPriorityListJComboBox.setSelectedItem(Settings.pushoverLightShakingPriorityList);

        pushoverPriorityLightShakingPanel.add(new JLabel("Priority for Light Shaking: "));
        pushoverPriorityLightShakingPanel.add(pushoverLightShakingPriorityListJComboBox);

        pushoverPriorityPanel.add(pushoverPriorityLightShakingPanel);

        JPanel pushoverPriorityStrongShakingPanel = new JPanel();
        pushoverPriorityStrongShakingPanel.setLayout(new BoxLayout(pushoverPriorityStrongShakingPanel, BoxLayout.X_AXIS));

        DefaultComboBoxModel<Integer> model3 = new DefaultComboBoxModel<>(new Integer[]{-1, 0, 1});

        pushoverStrongShakingPriorityListJComboBox = new JComboBox<>(model3);

        pushoverStrongShakingPriorityListJComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case -1: value = "Low"; break;
                        case 0: value = "Normal"; break;
                        case 1: value = "High"; break;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        pushoverStrongShakingPriorityListJComboBox.setSelectedItem(Settings.pushoverStrongShakingPriorityList);

        pushoverPriorityStrongShakingPanel.add(new JLabel("Priority for Strong Shaking: "));
        pushoverPriorityStrongShakingPanel.add(pushoverStrongShakingPriorityListJComboBox);

        pushoverPriorityPanel.add(pushoverPriorityStrongShakingPanel);

        panel.add(pushoverPriorityPanel);

        chkBoxPushoverCustomSounds = new JCheckBox("Use custom sounds for Pushover", Settings.usePushoverCustomSounds);
        textFieldPushoverSoundFeltLight = new JTextField(Settings.pushoverSoundFeltLight, 12);
        textFieldPushoverSoundFeltLight.setEnabled(chkBoxPushoverCustomSounds.isSelected());
        textFieldPushoverSoundFeltStrong = new JTextField(Settings.pushoverSoundFeltStrong, 12);
        textFieldPushoverSoundFeltStrong.setEnabled(chkBoxPushoverCustomSounds.isSelected());
        textFieldPushoverSoundDetected = new JTextField(Settings.pushoverSoundDetected, 12);
        textFieldPushoverSoundDetected.setEnabled(chkBoxPushoverCustomSounds.isSelected());
        chkBoxPushoverCustomSounds.addChangeListener(changeEvent -> {
            textFieldPushoverSoundFeltLight.setEnabled(chkBoxPushoverCustomSounds.isSelected());
            textFieldPushoverSoundFeltStrong.setEnabled(chkBoxPushoverCustomSounds.isSelected());
            textFieldPushoverSoundDetected.setEnabled(chkBoxPushoverCustomSounds.isSelected());
        });

        JPanel pushoverCustomSoundsPanel = new JPanel(new GridLayout(4, 1));
        pushoverCustomSoundsPanel.setBorder(BorderFactory.createTitledBorder("Custom Sounds"));

        JPanel pushoverUseCustomSoundsPanel = new JPanel();
        pushoverUseCustomSoundsPanel.setLayout(new BoxLayout(pushoverUseCustomSoundsPanel, BoxLayout.X_AXIS));
        pushoverUseCustomSoundsPanel.add(chkBoxPushoverCustomSounds);

        pushoverCustomSoundsPanel.add(pushoverUseCustomSoundsPanel);

        JPanel pushoverSoundDetectedPanel = new JPanel();
        pushoverSoundDetectedPanel.setLayout(new BoxLayout(pushoverSoundDetectedPanel, BoxLayout.X_AXIS));
        pushoverSoundDetectedPanel.add(new JLabel("Sound for Detected Earthquake: "));
        pushoverSoundDetectedPanel.add(textFieldPushoverSoundDetected);

        pushoverCustomSoundsPanel.add(pushoverSoundDetectedPanel);

        JPanel pushoverSoundFeltLightPanel = new JPanel();
        pushoverSoundFeltLightPanel.setLayout(new BoxLayout(pushoverSoundFeltLightPanel, BoxLayout.X_AXIS));
        pushoverSoundFeltLightPanel.add(new JLabel("Sound for Light Shaking: "));
        pushoverSoundFeltLightPanel.add(textFieldPushoverSoundFeltLight);

        pushoverCustomSoundsPanel.add(pushoverSoundFeltLightPanel);

        JPanel pushoverSoundFeltStrongPanel = new JPanel();
        pushoverSoundFeltStrongPanel.setLayout(new BoxLayout(pushoverSoundFeltStrongPanel, BoxLayout.X_AXIS));
        pushoverSoundFeltStrongPanel.add(new JLabel("Sound for Strong Shaking: "));
        pushoverSoundFeltStrongPanel.add(textFieldPushoverSoundFeltStrong);

        pushoverCustomSoundsPanel.add(pushoverSoundFeltStrongPanel);

        panel.add(pushoverCustomSoundsPanel);

        return panel;
    }

    @Override
    public void refreshUI() {
        chkBoxLocal.setText("Alert when any earthquake occurs closer than (%s): ".formatted(Settings.getSelectedDistanceUnit().getShortName()));
        label1.setText("and are closer from home location than (%s): ".formatted(Settings.getSelectedDistanceUnit().getShortName()));
        label2.setText("or is closer from home location than (%s): ".formatted(Settings.getSelectedDistanceUnit().getShortName()));
        chkBoxPossibleShaking.setText("Play sound if possible shaking is detected closer than (%s): ".formatted(Settings.getSelectedDistanceUnit().getShortName()));

        textFieldLocalDist.setText(String.format("%.1f", Settings.alertLocalDist * Settings.getSelectedDistanceUnit().getKmRatio()));
        textFieldRegionDist.setText(String.format("%.1f", Settings.alertRegionDist * Settings.getSelectedDistanceUnit().getKmRatio()));
        textFieldPossibleShakingDistance.setText(String.format("%.1f", Settings.alertPossibleShakingDistance * Settings.getSelectedDistanceUnit().getKmRatio()));
        textFieldQuakeMaxDist.setText(String.format("%.1f", Settings.earthquakeSoundsMaxDist * Settings.getSelectedDistanceUnit().getKmRatio()));

        revalidate();
        repaint();
    }

    @Override
    public void save() throws NumberFormatException {
        Settings.alertLocal = chkBoxLocal.isSelected();
        Settings.alertLocalDist = parseDouble(textFieldLocalDist.getText(), "Local alert distance", 0, 30000) / Settings.getSelectedDistanceUnit().getKmRatio();
        Settings.alertRegion = chkBoxRegion.isSelected();
        Settings.alertRegionMag = parseDouble(textFieldRegionMag.getText(), "Regional alert Magnitude", 0, 10);
        Settings.alertRegionDist = parseDouble(textFieldRegionDist.getText(), "Regional alert distance", 0, 30000) / Settings.getSelectedDistanceUnit().getKmRatio();

        Settings.alertGlobal = checkBoxGlobal.isSelected();
        Settings.alertGlobalMag = parseDouble(textFieldGlobalMag.getText(), "Global alert magnitude", 0, 10);
        Settings.focusOnEvent = chkBoxFocus.isSelected();
        Settings.jumpToAlert = chkBoxJumpToAlert.isSelected();

        Settings.shakingLevelScale = shakingThreshold.getShakingScaleComboBox().getSelectedIndex();
        Settings.shakingLevelIndex = shakingThreshold.getLevelComboBox().getSelectedIndex();

        Settings.strongShakingLevelScale = strongShakingThreshold.getShakingScaleComboBox().getSelectedIndex();
        Settings.strongShakingLevelIndex = strongShakingThreshold.getLevelComboBox().getSelectedIndex();

        Settings.alertPossibleShaking = chkBoxPossibleShaking.isSelected();
        Settings.alertPossibleShakingDistance = parseDouble(textFieldPossibleShakingDistance.getText(), "Possible shaking alert radius", 0, 30000) / Settings.getSelectedDistanceUnit().getKmRatio();
        Settings.enableEarthquakeSounds = chkBoxEarthquakeSounds.isSelected();
        Settings.earthquakeSoundsMinMagnitude = parseDouble(textFieldQuakeMinMag.getText(), "Earthquake minimum magnitude to play sound", 0, 10);
        Settings.earthquakeSoundsMaxDist = parseDouble(textFieldQuakeMaxDist.getText(), "Earthquake maximum distance to play sound", 0, 30000) / Settings.getSelectedDistanceUnit().getKmRatio();

        Settings.eewScale = eewThreshold.getShakingScaleComboBox().getSelectedIndex();
        Settings.eewLevelIndex = eewThreshold.getLevelComboBox().getSelectedIndex();
        Settings.eewClusterLevel = (Integer) comboBoxEEWClusterLevel.getSelectedItem();

        Settings.useNtfy = chkBoxNtfy.isSelected();
        Settings.usePushover = chkBoxPushover.isSelected();
        Settings.ntfy = textFieldNtfy.getText();
        Settings.pushoverUserID = textFieldPushoverUserID.getText();
        Settings.pushoverToken = textFieldPushoverToken.getText();
        Settings.usePushoverCustomSounds = chkBoxPushoverCustomSounds.isSelected();
        Settings.pushoverSoundDetected = textFieldPushoverSoundDetected.getText();
        Settings.pushoverSoundFeltLight = textFieldPushoverSoundFeltLight.getText();
        Settings.pushoverSoundFeltStrong = textFieldPushoverSoundFeltStrong.getText();
        Settings.ntfyNearbyShaking = chkBoxNtfyNearbyShaking.isSelected();
        Settings.ntfyFeltShaking = chkBoxNtfyFeltShaking.isSelected();
        Settings.ntfyNearbyShakingPriorityList = (Integer) ntfyNearbyShakingPriorityListJComboBox.getSelectedItem();
        Settings.ntfyLightShakingPriorityList = (Integer) ntfyLightShakingPriorityListJComboBox.getSelectedItem();
        Settings.ntfyStrongShakingPriorityList = (Integer) ntfyStrongShakingPriorityListJComboBox.getSelectedItem();
        Settings.pushoverNearbyShaking = chkBoxPushoverNearbyShaking.isSelected();
        Settings.pushoverFeltShaking = chkBoxPushoverFeltShaking.isSelected();
        Settings.pushoverNearbyShakingPriorityList = (Integer) pushoverNearbyShakingPriorityListJComboBox.getSelectedItem();
        Settings.pushoverLightShakingPriorityList = (Integer) pushoverLightShakingPriorityListJComboBox.getSelectedItem();
        Settings.pushoverStrongShakingPriorityList = (Integer) pushoverStrongShakingPriorityListJComboBox.getSelectedItem();
        Settings.ntfySendRevisions = chkBoxNtfySendRevisions.isSelected();
        Settings.pushoverSendRevisions = chkBoxPushoverSendRevisions.isSelected();
    }

    @Override
    public String getTitle() {
        return "Alerts";
    }
}
