/**
 * 
 */
package megameklab.com.ui.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.QuadVee;
import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.util.EncodeControl;
import megameklab.com.ui.util.CustomComboBox;
import megameklab.com.ui.util.TechComboBox;

/**
 * Construction options and systems for Meks.
 * 
 * @author Neoancient
 *
 */
public class MekChassisView extends JPanel implements ActionListener, ChangeListener {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2620071922845931509L;

    public interface MekChassisListener {
        void refreshSummary();
        void tonnageChanged(double tonnage);
        void omniChanged(boolean omni);
        void typeChanged(int baseType, int motiveType, long etype);
        void structureChanged(EquipmentType structure);
        void engineChanged(Engine engine);
        void gyroChanged(int gyroType);
        void cockpitChanged(int cockpitType);
        void enhancementChanged(EquipmentType enhancement);
        void fullHeadEjectChanged(boolean eject);
        void resetChassis();
    }
    List<MekChassisListener> listeners = new CopyOnWriteArrayList<>();
    public void addListener(MekChassisListener l) {
        listeners.add(l);
    }
    public void removeListener(MekChassisListener l) {
        listeners.remove(l);
    }
    
    public static final int BASE_TYPE_STANDARD = 0;
    public static final int BASE_TYPE_LAM      = 1;
    public static final int BASE_TYPE_QUADVEE  = 2;
    
    public static final int MOTIVE_TYPE_BIPED  = 0;
    public static final int MOTIVE_TYPE_QUAD   = 1;
    public static final int MOTIVE_TYPE_TRIPOD = 2;
    
    public static final int MOTIVE_TYPE_LAM_STD  = 0;
    public static final int MOTIVE_TYPE_LAM_BM   = 1;
    
    public static final int MOTIVE_TYPE_QV_TRACKED   = 0;
    public static final int MOTIVE_TYPE_QV_WHEELED   = 1;
    
    // Engines that can be used by mechs and the order they appear in the combobox
    private final static int[] ENGINE_TYPES = {
            Engine.NORMAL_ENGINE, Engine.XL_ENGINE, Engine.XXL_ENGINE, Engine.FUEL_CELL, Engine.LIGHT_ENGINE,
            Engine.COMPACT_ENGINE, Engine.FISSION, Engine.COMBUSTION_ENGINE
    };
    // Primitive Mechs can only use some engine types. These are also the only ones available to IndusrialMechs
    // under standard rules.
    private final static int[] PRIMITIVE_ENGINE_TYPES = {
            Engine.NORMAL_ENGINE, Engine.FUEL_CELL, Engine.FISSION, Engine.COMBUSTION_ENGINE
    };
    // LAMs can only use fusion engines that are contained entirely within the center torso.
    private final static int[] LAM_ENGINE_TYPES = {
            Engine.NORMAL_ENGINE, Engine.COMPACT_ENGINE
    };
    
    // Internal structure for non-industrial mechs
    private final static int[] STRUCTURE_TYPES = {
            EquipmentType.T_STRUCTURE_STANDARD, EquipmentType.T_STRUCTURE_ENDO_STEEL,
            EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE, EquipmentType.T_STRUCTURE_REINFORCED,
            EquipmentType.T_STRUCTURE_COMPOSITE, EquipmentType.T_STRUCTURE_ENDO_COMPOSITE
    };

    // Internal structure for superheavy battlemechs
    private final static int[] SUPERHEAVY_STRUCTURE_TYPES = {
            EquipmentType.T_STRUCTURE_STANDARD, EquipmentType.T_STRUCTURE_ENDO_STEEL,
            EquipmentType.T_STRUCTURE_ENDO_COMPOSITE
    };

    final private SpinnerNumberModel tonnageModel = new SpinnerNumberModel(20, 20, 100, 5);
    final private JSpinner spnTonnage = new JSpinner(tonnageModel);
    final private JCheckBox chkOmni = new JCheckBox("Omni");
    final private JComboBox<String> cbBaseType = new JComboBox<>();
    final private JComboBox<String> cbMotiveType = new JComboBox<String>();
    final private TechComboBox<EquipmentType> cbStructure = new TechComboBox<>(EquipmentType::getName);
    final private TechComboBox<Engine> cbEngine = new TechComboBox<>(e -> e.getEngineName().replaceAll("^\\d+ ", ""));
    final private CustomComboBox<Integer> cbGyro = new CustomComboBox<>(g -> Mech.getGyroTypeShortString(g));
    final private CustomComboBox<Integer> cbCockpit = new CustomComboBox<>(c -> Mech.getCockpitTypeString(c));
    final private TechComboBox<EquipmentType> cbEnhancement = new TechComboBox<>(EquipmentType::getName);
    final private JCheckBox chkFullHeadEject = new JCheckBox();
    final private JButton btnResetChassis = new JButton();    
    
    private ComboBoxModel<String> standardTypesModel;
    private ComboBoxModel<String> lamTypesModel;
    private ComboBoxModel<String> qvTypesModel;
    
    private boolean primitive = false;
    private boolean industrial = false;
    private int engineRating = 20;
    
    private static final int[] GENERAL_COCKPITS = {
            Mech.COCKPIT_STANDARD, Mech.COCKPIT_SMALL, Mech.COCKPIT_COMMAND_CONSOLE,
            Mech.COCKPIT_TORSO_MOUNTED, Mech.COCKPIT_DUAL, Mech.COCKPIT_INTERFACE,
            Mech.COCKPIT_VRRP
    };
    
    private static final String[] ENHANCEMENT_NAMES = {
            "ISMASC", "CLMASC", "TSM", "ISSuperCooledMyomer"
    };
    
    final ITechManager techManager;
    
    public MekChassisView(ITechManager techManager) {
        this.techManager = techManager;
        initUI();
    }

    private void initUI() {
        Dimension labelSize = new Dimension(110, 25);
        Dimension controlSize = new Dimension(180, 25);
        Dimension spinnerSize = new Dimension(55, 25);

        ResourceBundle resourceMap = ResourceBundle.getBundle("megameklab.resources.Views", new EncodeControl()); //$NON-NLS-1$
        cbBaseType.setModel(new DefaultComboBoxModel<>(resourceMap.getString("MekChassisView.baseType.values").split(","))); //$NON-NLS-1$
        standardTypesModel = new DefaultComboBoxModel<>(resourceMap.getString("MekChassisView.motiveType.values").split(",")); //$NON-NLS-1$
        lamTypesModel = new DefaultComboBoxModel<>(resourceMap.getString("MekChassisView.lamType.values").split(",")); //$NON-NLS-1$
        qvTypesModel = new DefaultComboBoxModel<>(resourceMap.getString("MekChassisView.qvType.values").split(",")); //$NON-NLS-1$
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(createLabel(resourceMap.getString("MekChassisView.spnTonnage.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 0;
        setFieldSize(spnTonnage, spinnerSize);
        add(spnTonnage, gbc);
        spnTonnage.addChangeListener(this);
        
        add(spnTonnage, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        chkOmni.setText(resourceMap.getString("MekChassisView.chkOmni.text")); //$NON-NLS-1$
        add(chkOmni, gbc);
        chkOmni.addChangeListener(this);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbBaseType.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        setFieldSize(cbBaseType, controlSize);
        add(cbBaseType, gbc);
        cbBaseType.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbMotiveType.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        setFieldSize(cbMotiveType, controlSize);
        add(cbMotiveType, gbc);
        cbMotiveType.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbStructure.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        setFieldSize(cbStructure, controlSize);
        add(cbStructure, gbc);
        cbStructure.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbEngine.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        setFieldSize(cbEngine, controlSize);
        add(cbEngine, gbc);
        cbEngine.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbGyro.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        setFieldSize(cbGyro, controlSize);
        add(cbGyro, gbc);
        cbGyro.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbCockpit.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        setFieldSize(cbCockpit, controlSize);
        add(cbCockpit, gbc);
        cbCockpit.addActionListener(this);
        
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("MekChassisView.cbEnhancement.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        setFieldSize(cbEnhancement, controlSize);
        cbEnhancement.setNullValue(resourceMap.getString("MekChassisView.cbEnhancement.null")); //$NON-NLS-1$
        add(cbEnhancement, gbc);
        cbEnhancement.addActionListener(this);
        
        chkFullHeadEject.setText(resourceMap.getString("MekChassisView.chkFullHeadEject.text")); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        add(chkFullHeadEject, gbc);
        chkFullHeadEject.addActionListener(this);
        
        btnResetChassis.setText(resourceMap.getString("MekChassisView.btnResetChassis.text")); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = 9;
        gbc.gridwidth = 3;
        add(btnResetChassis, gbc);
        btnResetChassis.addActionListener(this);
    }

    public JLabel createLabel(String text, Dimension maxSize) {

        JLabel label = new JLabel(text, SwingConstants.RIGHT);

        setFieldSize(label, maxSize);
        return label;
    }

    public void setFieldSize(JComponent box, Dimension maxSize) {
        box.setPreferredSize(maxSize);
        box.setMaximumSize(maxSize);
        box.setMinimumSize(maxSize);
    }
    
    public void setFromEntity(Mech mech) {
        primitive = mech.isPrimitive();
        industrial = mech.isIndustrial();
        engineRating = mech.getEngine().getRating();
        refresh();
        setTonnage(mech.getWeight());
        setOmni(mech.isOmni());
        chkOmni.setEnabled(!mech.isPrimitive() && techManager.isLegal(Entity.getOmniAdvancement()));
        cbBaseType.setEnabled(!primitive && !industrial);
        if (mech instanceof LandAirMech) {
            chkOmni.setEnabled(false);
            setBaseTypeIndex(BASE_TYPE_LAM);
            cbMotiveType.setModel(lamTypesModel);
            setMotiveTypeIndex(((LandAirMech)mech).getLAMType());
        } else if (mech instanceof QuadVee) {
            setBaseTypeIndex(BASE_TYPE_QUADVEE);
            cbMotiveType.setModel(qvTypesModel);
            setMotiveTypeIndex(((QuadVee)mech).getMotiveType());
        } else {
            setBaseTypeIndex(BASE_TYPE_STANDARD);
            cbMotiveType.setModel(standardTypesModel);
            if ((mech.getEntityType() & Entity.ETYPE_TRIPOD_MECH) != 0) {
                setMotiveTypeIndex(MOTIVE_TYPE_TRIPOD);
            } else if ((mech.getEntityType() & Entity.ETYPE_QUAD_MECH) != 0) {
                setMotiveTypeIndex(MOTIVE_TYPE_QUAD);
            } else {
                setMotiveTypeIndex(MOTIVE_TYPE_BIPED);
            }
        }
        setStructureType(EquipmentType.getStructureTypeName(mech.getStructureType(),
                TechConstants.isClan(mech.getStructureTechLevel())));
        setEngine(mech.getEngine());
        setGyroType(mech.getGyroType());
        setCockpitType(mech.getCockpitType());
        // A simple hasWorkingMisc() will not tell us whether we have IS or Clan MASC, so we need to search
        // the list for the first matching.
        Optional<EquipmentType> enh = mech.getMisc().stream().map(Mounted::getType)
                .filter(et -> (et.hasFlag(MiscType.F_MASC) && et.getSubType() == 0)
                    || et.hasFlag(MiscType.F_TSM)
                    || et.hasFlag(MiscType.F_INDUSTRIAL_TSM)
                    || et.hasFlag(MiscType.F_SCM)).findFirst();
        if (enh.isPresent()) {
            setEnhancement(enh.get());
        } else {
            setEnhancement(null);
        }
        setFullHeadEject(mech.hasFullHeadEject());
        btnResetChassis.setEnabled(mech.isOmni());
    }
    
    public void setAsCustomization() {
        spnTonnage.setEnabled(false);
        cbBaseType.setEnabled(false);
        cbMotiveType.setEnabled(false);
    }
    
    public boolean isSuperheavy() {
        return getTonnage() > 100;
    }
    
    public boolean isPrimitive() {
        return primitive;
    }
    
    public boolean isIndustrial() {
        return industrial;
    }
    
    public int getEngineRating() {
        return engineRating;
    }
    
    public void setEngineRating(int rating) {
        engineRating = rating;
    }
    
    public void refresh() {
        refreshTonnage();
        refreshStructure();
        refreshEngine();
        refreshGyro();
        refreshCockpit();
        refreshEnhancement();
        refreshFullHeadEject();
        
        chkOmni.removeActionListener(this);
        chkOmni.setEnabled(!isPrimitive() && !isIndustrial()
                && techManager.isLegal(Entity.getOmniAdvancement()));
        chkOmni.addActionListener(this);
    }

    private void refreshTonnage() {
        int prev = tonnageModel.getNumber().intValue();
        int min = 20;
        int max = 100;
        spnTonnage.removeChangeListener(this);
        if (getBaseTypeIndex() == BASE_TYPE_LAM) {
            max = 55;
        } else if ((getBaseTypeIndex() == BASE_TYPE_STANDARD)
                && techManager.isLegal(Mech.getTechAdvancement(Entity.ETYPE_MECH, false, false,
                        EntityWeightClass.WEIGHT_SUPER_HEAVY))) {
            max = 200;;
        }
        if (techManager.isLegal(Mech.getTechAdvancement(Entity.ETYPE_MECH, false, false,
                EntityWeightClass.WEIGHT_ULTRA_LIGHT))) {
            min = 10;
        }
        tonnageModel.setMinimum(min);
        tonnageModel.setMaximum(max);
        spnTonnage.addChangeListener(this);
        if ((prev < min) || (prev > max)) {
            spnTonnage.setValue(prev);
        }
    }
    
    private void refreshStructure() {
        boolean isMixed = techManager.isMixedTech();
        boolean isClan = techManager.isClan();
        cbStructure.removeActionListener(this);
        EquipmentType prevStructure = (EquipmentType)cbStructure.getSelectedItem();
        cbStructure.removeAllItems();
        cbStructure.showTechBase(isMixed);
        // Primitive/retro can only use standard/industrial structure. Industrial can only use industrial
        // at standard rules level. Superheavies can only use standard.
        if (isIndustrial()) {
            String name = EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_INDUSTRIAL, isClan);
            cbStructure.addItem(EquipmentType.get(name));
        } else if (isPrimitive()) {
            String name = EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_STANDARD, isClan);
            cbStructure.addItem(EquipmentType.get(name));
        } else {
            int[] structureTypes = isSuperheavy()?
                    SUPERHEAVY_STRUCTURE_TYPES : STRUCTURE_TYPES;
            for (int i : structureTypes) {
                String name = EquipmentType.getStructureTypeName(i, isClan);
                EquipmentType structure = EquipmentType.get(name);
                // LAMs cannot use bulky structure
                if ((getBaseTypeIndex() == BASE_TYPE_LAM) && (structure.getCriticals(null) > 0)) {
                    continue;
                }
                if ((null != structure) && techManager.isLegal(structure)) {
                    cbStructure.addItem(structure);
                }
                if (isMixed && (i > EquipmentType.T_STRUCTURE_INDUSTRIAL)) {
                    name = EquipmentType.getStructureTypeName(i, !isClan);
                    structure = EquipmentType.get(name);
                    if ((null != structure) && techManager.isLegal(structure)) {
                        cbStructure.addItem(structure);
                    }
                }
            }
        }
        cbStructure.setSelectedItem(prevStructure);
        cbStructure.addActionListener(this);
        if (cbStructure.getSelectedIndex() < 0) {
            cbStructure.setSelectedIndex(0);
        }
    }

    private void refreshEngine() {
        boolean isMixed = techManager.isMixedTech();
        cbEngine.removeActionListener(this);
        Engine prevEngine = (Engine)cbEngine.getSelectedItem();
        int prevType = null == prevEngine? -1 : prevEngine.getEngineType();
        int prevFlags = null == prevEngine? -1 : prevEngine.getFlags();
        cbEngine.removeAllItems();
        int flags = 0;
        if (techManager.isClan()) {
            flags |= Engine.CLAN_ENGINE;
        }
        if (getEngineRating() > 400) {
            flags |= Engine.LARGE_ENGINE;
        }
        int altFlags = flags ^ Engine.CLAN_ENGINE;
        int[] engineTypes = ENGINE_TYPES;
        if (isPrimitive() || (isIndustrial()
                && techManager.getTechLevel().compareTo(SimpleTechLevel.EXPERIMENTAL) < 0)) {
            engineTypes = PRIMITIVE_ENGINE_TYPES;
        } else if (getBaseTypeIndex() == BASE_TYPE_LAM) {
            engineTypes = LAM_ENGINE_TYPES;
        }
        // Primitive and industrial mechs can use non-fusion engines, as can non-superheavies under experimental rules
        boolean allowNonFusion = !isSuperheavy()
                && (isIndustrial() || isPrimitive()
                        || (techManager.getTechLevel().compareTo(SimpleTechLevel.EXPERIMENTAL) >= 0));
        int sameEngine = -1;
        int index = 0;
        for (int i : engineTypes) {
            Engine e = new Engine(getEngineRating(), i, flags);
            if (e.engineValid && (e.isFusion() || allowNonFusion)) {
                cbEngine.addItem(e);
                if ((e.getEngineType() == prevType) && (e.getFlags() == prevFlags)) {
                    sameEngine = index;
                }
                index++;
            }
            // Only add the opposite tech base if the engine is different.
            if (isMixed && e.getSideTorsoCriticalSlots().length > 0) {
                e = new Engine(getEngineRating(), i, altFlags);
                if (e.engineValid && (e.isFusion()
                        || techManager.getTechLevel().compareTo(SimpleTechLevel.EXPERIMENTAL) >= 0)) {
                    cbEngine.addItem(e);
                    if ((e.getEngineType() == prevType) && (e.getFlags() == prevFlags)) {
                        sameEngine = index;
                    }
                    index++;
                }
            }
        }
        cbEngine.setSelectedIndex(sameEngine);
        cbEngine.addActionListener(this);
        if (cbEngine.getSelectedIndex() < 0) {
            cbEngine.setSelectedIndex(0);
        }
    }
    
    private void refreshGyro() {
        cbGyro.removeActionListener(this);
        Integer prev = (Integer)cbGyro.getSelectedItem();
        cbGyro.removeAllItems();
        if (isSuperheavy()) {
            cbGyro.addItem(Mech.GYRO_SUPERHEAVY);
        } else if (isPrimitive() || isIndustrial()) {
            cbGyro.addItem(Mech.GYRO_STANDARD);
        } else {
            for (int i = 0; i <= Mech.GYRO_NONE; i++) {
                if (techManager.isLegal(Mech.getGyroTechAdvancement(i))
                        && ((i != Mech.GYRO_XL) || (getBaseTypeIndex() !=  BASE_TYPE_LAM))) {
                    cbGyro.addItem(i);
                }
            }
        }
        cbGyro.setSelectedItem(prev);
        cbGyro.addActionListener(this);
        if (cbGyro.getSelectedIndex() < 0) {
            cbGyro.setSelectedIndex(0);
        }
    }
    
    private void refreshCockpit() {
        cbCockpit.removeActionListener(this);
        Integer prev = (Integer)cbCockpit.getSelectedItem();
        cbCockpit.removeAllItems();
        if ((getBaseTypeIndex() == BASE_TYPE_STANDARD) && (getMotiveTypeIndex() == MOTIVE_TYPE_TRIPOD)) {
            cbCockpit.addItem(isSuperheavy()? Mech.COCKPIT_SUPERHEAVY_TRIPOD : Mech.COCKPIT_TRIPOD);
        } else if (getBaseTypeIndex() == BASE_TYPE_LAM) {
            cbCockpit.addItem(Mech.COCKPIT_STANDARD);
            cbCockpit.addItem(Mech.COCKPIT_SMALL);
        } else if (getBaseTypeIndex() == BASE_TYPE_QUADVEE) {
            cbCockpit.addItem(Mech.COCKPIT_QUADVEE);
        } else if (isSuperheavy()) {
            cbCockpit.addItem(isIndustrial()? Mech.COCKPIT_SUPERHEAVY_INDUSTRIAL : Mech.COCKPIT_SUPERHEAVY);
        } else if (isPrimitive()) {
            cbCockpit.addItem(isIndustrial()? Mech.COCKPIT_PRIMITIVE_INDUSTRIAL : Mech.COCKPIT_PRIMITIVE);
        } else if (isIndustrial()) {
            cbCockpit.addItem(Mech.COCKPIT_INDUSTRIAL);
        } else {
            for (int cockpitType : GENERAL_COCKPITS) {
                if (techManager.isLegal(Mech.getCockpitTechAdvancement(cockpitType))) {
                    cbCockpit.addItem(cockpitType);
                }
            }
        }
        cbCockpit.setSelectedItem(prev);
        cbCockpit.addActionListener(this);
        if (cbCockpit.getSelectedIndex() < 0) {
            cbCockpit.setSelectedIndex(0);
        }
    }
    
    private void refreshEnhancement() {
        cbEnhancement.removeActionListener(this);
        EquipmentType prev = (EquipmentType)cbEnhancement.getSelectedItem();
        cbEnhancement.removeAllItems();
        cbEnhancement.addItem(null);
        if (!isSuperheavy() && !isPrimitive()) {
            if (isIndustrial()) {
                EquipmentType eq = EquipmentType.get("Industrial TSM"); //$NON-NLS-1$
                if (techManager.isLegal(eq)) {
                    cbEnhancement.addItem(eq);
                }
            } else {
                cbEnhancement.showTechBase(techManager.isMixedTech());
                for (String name : ENHANCEMENT_NAMES) {
                    EquipmentType eq = EquipmentType.get(name);
                    if (techManager.isLegal(eq)) {
                        cbEnhancement.addItem(eq);
                    }
                }
            }
        }
        cbEnhancement.setSelectedItem(prev);
        cbEnhancement.addActionListener(this);
        if (cbEnhancement.getSelectedIndex() < 0) {
            cbEnhancement.setSelectedIndex(0);
        }
    }
    
    private void refreshFullHeadEject() {
        chkFullHeadEject.removeActionListener(this);
        chkFullHeadEject.setEnabled((getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)
                && (getCockpitType() != Mech.COCKPIT_VRRP)
                && (getCockpitType() != Mech.COCKPIT_COMMAND_CONSOLE)
                && techManager.isLegal(Mech.getFullHeadEjectAdvancement()));
        chkFullHeadEject.addActionListener(this);
    }
    
    public double getTonnage() {
        return tonnageModel.getNumber().doubleValue();
    }
    
    public void setTonnage(double tonnage) {
        spnTonnage.setValue(new Integer((int)Math.ceil(tonnage)));
    }
    
    public boolean isOmni() {
        return chkOmni.isSelected() && chkOmni.isEnabled();
    }
    
    public void setOmni(boolean omni) {
        chkOmni.setSelected(omni);
    }
    
    public int getBaseTypeIndex() {
        return cbBaseType.getSelectedIndex();
    }
    
    public void setBaseTypeIndex(int index) {
        cbBaseType.setSelectedIndex(index);
    }
    
    public long getEntityType() {
        if (getBaseTypeIndex() == BASE_TYPE_LAM) {
            return Entity.ETYPE_LAND_AIR_MECH;
        } else if (getBaseTypeIndex() == BASE_TYPE_QUADVEE) {
            return Entity.ETYPE_QUADVEE;
        } else if (getMotiveTypeIndex() == MOTIVE_TYPE_TRIPOD) {
            return Entity.ETYPE_TRIPOD_MECH;
        } else if (getMotiveTypeIndex() == MOTIVE_TYPE_QUAD) {
            return Entity.ETYPE_QUAD_MECH;
        } else {
            return Entity.ETYPE_BIPED_MECH;
        }
    }

    public int getMotiveTypeIndex() {
        return cbMotiveType.getSelectedIndex();
    }
    
    public void setMotiveTypeIndex(int index) {
        cbMotiveType.setSelectedIndex(index);
    }
    
    public EquipmentType getStructure() {
        return (EquipmentType)cbStructure.getSelectedItem();
    }
    
    public void setStructureType(EquipmentType structure) {
        cbStructure.setSelectedItem(structure);
    }
    
    public void setStructureType(String structureName) {
        EquipmentType structure = EquipmentType.get(structureName);
        cbStructure.setSelectedItem(structure);
    }

    public Engine getEngine() {
        Engine e = (Engine) cbEngine.getSelectedItem();
        return new Engine(getEngineRating(), e.getEngineType(), e.getFlags());
    }
    
    public void setEngine(Engine engine) {
        cbEngine.setSelectedItem(engine);
    }
    
    public int getGyroType() {
        return (Integer)cbGyro.getSelectedItem();
    }
    
    public void setGyroType(int gyro) {
        cbGyro.setSelectedItem(gyro);
    }

    public int getCockpitType() {
        return (Integer)cbCockpit.getSelectedItem();
    }
    
    public void setCockpitType(int cockpit) {
        cbCockpit.setSelectedItem(cockpit);
    }
    
    public EquipmentType getEnhancement() {
        return (EquipmentType)cbEnhancement.getSelectedItem();
    }
    
    public void setEnhancement(EquipmentType enhancement) {
        cbEnhancement.setSelectedItem(enhancement);
    }
    
    public boolean hasFullHeadEject() {
        return chkFullHeadEject.isSelected() && chkFullHeadEject.isEnabled();
    }
    
    public void setFullHeadEject(boolean eject) {
        chkFullHeadEject.setSelected(eject);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chkOmni) {
            listeners.forEach(l -> l.omniChanged(isOmni()));
        } else if ((e.getSource() == cbBaseType) || (e.getSource() == cbMotiveType)) {
            listeners.forEach(l -> l.typeChanged(getBaseTypeIndex(), getMotiveTypeIndex(), getEntityType()));
        } else if (e.getSource() == cbStructure) {
            listeners.forEach(l -> l.structureChanged(getStructure()));
        } else if (e.getSource() == cbEngine) {
            listeners.forEach(l -> l.engineChanged(getEngine()));
        } else if (e.getSource() == cbGyro) {
            listeners.forEach(l -> l.gyroChanged(getGyroType()));
        } else if (e.getSource() == cbCockpit) {
            listeners.forEach(l -> l.cockpitChanged(getCockpitType()));
            refreshFullHeadEject();
        } else if (e.getSource() == cbEnhancement) {
            listeners.forEach(l -> l.enhancementChanged(getEnhancement()));
        } else if (e.getSource() == chkFullHeadEject) {
            listeners.forEach(l -> l.fullHeadEjectChanged(chkFullHeadEject.isSelected()));
        } else if (e.getSource() == btnResetChassis) {
            listeners.forEach(MekChassisListener::resetChassis);
        }
        refresh();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == spnTonnage) {
            listeners.forEach(l -> l.tonnageChanged(getTonnage()));
        }
        // Change from standard to superheavy or reverse will cause the structure tab to call setEntity()
        // and so cause a refresh
    }
}
