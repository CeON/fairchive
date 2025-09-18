package edu.harvard.iq.dataverse.search.geonames;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.Arrays.binarySearch;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.solr.client.solrj.beans.Field;

public class GeoName {    
    
    private static final int MAX_ALT_NAMES_LENGTH = 50;
    
    private int id;
    @Field
    private String name;
    @Field
    private String alternateNames;
    @Field
    private String featureCode;
    private String countryCode;
    private String admin1Code;
    private String admin2Code;
    private String admin3Code;
    private String admin4Code;
    @Field
    private String hierarchy;

    public String getId() {
        return Integer.toString(this.id);
    }

    @Field
    public void setId(final String id) {
        this.id = Integer.parseInt(id);
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getAlternateNames() {
        return this.alternateNames;
    }

    public void setAlternateNames(final String alternateNames) {
        this.alternateNames = alternateNames;
    }

    public String getFeatureCode() {
        return this.featureCode;
    }

    public void setFeatureCode(final String featureCode) {
        this.featureCode = featureCode;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAdmin1Code() {
        return this.admin1Code;
    }

    public void setAdmin1Code(final String admin1Code) {
        this.admin1Code = admin1Code;
    }

    public String getAdmin2Code() {
        return this.admin2Code;
    }

    public void setAdmin2Code(final String admin2Code) {
        this.admin2Code = admin2Code;
    }

    public String getAdmin3Code() {
        return this.admin3Code;
    }

    public void setAdmin3Code(final String admin3Code) {
        this.admin3Code = admin3Code;
    }

    public String getAdmin4Code() {
        return this.admin4Code;
    }

    public void setAdmin4Code(final String admin4Code) {
        this.admin4Code = admin4Code;
    }

    boolean isTier0() {
        return this.admin1Code == null;
    }

    boolean isTier1() {
        return this.admin1Code != null & this.admin2Code == null;
    }

    boolean isTier2() {
        return this.admin2Code != null & this.admin3Code == null;
    }

    boolean isTier3() {
        return this.admin3Code != null & this.admin4Code == null;
    }

    boolean isTier4() {
        return this.admin4Code != null;
    }

    boolean isAdm1() {
        return this.featureCode.startsWith("ADM1");
    }

    boolean isAdm2() {
        return this.featureCode.startsWith("ADM2");
    }

    boolean isAdm3() {
        return this.featureCode.startsWith("ADM3");
    }

    boolean isAdm4() {
        return this.featureCode.startsWith("ADM4");
    }

    boolean isAdm5() {
        return this.featureCode.startsWith("ADM5");
    }

    public String getHierarchy() {
        return this.hierarchy;
    }

    public void setHierarchy(final String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public String getDetails(final String beginDecorator, final String endDecorator,
            final String separator) {
        final StringBuilder result = new StringBuilder(80);
        result.append(beginDecorator).append(getStringFromBundle("geoname.id"))
                .append(endDecorator).append(": ").append(this.id)
                .append(separator);
        result.append(beginDecorator).append(getStringFromBundle("geoname.name"))
                .append(endDecorator).append(": ")
                .append(this.name).append(separator);
        result.append(beginDecorator).append(getStringFromBundle("geoname.hierarchy"))
                .append(endDecorator).append(": ")
                .append(this.hierarchy).append(separator);
        if (isNotBlank(this.alternateNames)) {
            result.append(beginDecorator)
                    .append(getStringFromBundle("geoname.altnames"))
                    .append(endDecorator).append(": ");
            if (this.alternateNames.length() > MAX_ALT_NAMES_LENGTH) {
                result.append(this.alternateNames, 0, MAX_ALT_NAMES_LENGTH)
                        .append(" ...");
            } else {
                result.append(this.alternateNames);
            }
            result.append(separator);
        }
        result.append(beginDecorator)
                .append(getStringFromBundle("geonames.featurecode"))
                .append(endDecorator).append(": ")
                .append(this.featureCode);
        return result.toString();
    }

    public String getDetails() {
        return getDetails(EMPTY, EMPTY, " ");
    }

    public String getDetailsHTML() {
        return getDetails("<b>", "</b>", " ");
    }

    @Override
    public String toString() {
        return Integer.toString(this.id);
    }
    
    public static boolean isFeatureCode(final String txt) {
        return binarySearch(featureCodes, txt.toUpperCase()) > 0;
    }

    private static String[] featureCodes = { "ADM1", "ADM1H", "ADM2", "ADM2H", "ADM3",
            "ADM3H", "ADM4", "ADM4H", "ADM5", "ADM5H", "ADMD", "ADMDH", "ADMF", "ADMS",
            "AGRC", "AGRF", "AIRB", "AIRF", "AIRH", "AIRP", "AIRQ", "AIRS", "AIRT",
            "AMTH", "AMUS", "ANCH", "ANS", "APNU", "AQC", "ARCH", "ARCHV", "ARCU",
            "AREA", "ARRU", "ART", "ASPH", "ASTR", "ASYL", "ATHF", "ATM", "ATOL",
            "BANK", "BAR", "BAY", "BAYS", "BCH", "BCHS", "BCN", "BDG", "BDGQ", "BDLD",
            "BDLU", "BGHT", "BKSU", "BLDA", "BLDG", "BLDO", "BLDR", "BLHL", "BLOW",
            "BNCH", "BNK", "BNKR", "BNKU", "BNKX", "BOG", "BP", "BRKS", "BRKW", "BSND",
            "BSNP", "BSNU", "BSTN", "BTL", "BTYD", "BUR", "BUSH", "BUSTN", "BUSTP",
            "BUTE", "CAPE", "CAPG", "CARN", "CAVE", "CDAU", "CFT", "CH", "CHN", "CHNL",
            "CHNM", "CHNN", "CLDA", "CLF", "CLG", "CMN", "CMP", "CMPL", "CMPLA",
            "CMPMN", "CMPO", "CMPQ", "CMPRF", "CMTY", "CNFL", "CNL", "CNLA", "CNLB",
            "CNLD", "CNLI", "CNLN", "CNLQ", "CNLSB", "CNLX", "CNS", "CNSU", "CNYN",
            "CNYU", "COLF", "COMC", "CONE", "CONT", "COVE", "CRDR", "CRKT", "CRNT",
            "CRQ", "CRQS", "CRRL", "CRSU", "CRTR", "CSNO", "CST", "CSTL", "CSTM",
            "CSWY", "CTHSE", "CTRA", "CTRB", "CTRCM", "CTRF", "CTRM", "CTRR", "CTRS",
            "CTYD", "CUET", "CULT", "CUTF", "CVNT", "DAM", "DAMQ", "DAMSB", "DARY",
            "DCK", "DCKB", "DCKD", "DCKY", "DEPU", "DEVH", "DIKE", "DIP", "DLTA",
            "DOMG", "DPOF", "DPR", "DPRG", "DSRT", "DTCH", "DTCHD", "DTCHI", "DTCHM",
            "DUNE", "DVD", "EDGU", "ERG", "ESCU", "EST", "ESTO", "ESTR", "ESTSG",
            "ESTT", "ESTX", "ESTY", "FAN", "FANU", "FCL", "FIRE", "FISH", "FJD",
            "FJDS", "FLD", "FLDI", "FLLS", "FLLSX", "FLTM", "FLTT", "FLTU", "FNDY",
            "FORD", "FRM", "FRMQ", "FRMS", "FRMT", "FRST", "FRSTF", "FRZU", "FSR",
            "FT", "FURU", "FY", "FYT", "GAP", "GAPU", "GASF", "GATE", "GDN", "GHAT",
            "GHSE", "GLCR", "GLYU", "GOSP", "GOVL", "GRAZ", "GRGE", "GROVE", "GRSLD",
            "GRVC", "GRVE", "GRVO", "GRVP", "GRVPN", "GULF", "GVL", "GYSR", "HBR",
            "HBRX", "HDLD", "HERM", "HLL", "HLLS", "HLLU", "HLSU", "HLT", "HMCK",
            "HMDA", "HMSD", "HOLU", "HSE", "HSEC", "HSP", "HSPC", "HSPD", "HSPL",
            "HSTS", "HTH", "HTL", "HUT", "HUTS", "INDS", "INLT", "INLTQ", "INSM",
            "INTF", "ISL", "ISLET", "ISLF", "ISLM", "ISLS", "ISLT", "ISLX", "ISTH",
            "ITTR", "JTY", "KNLU", "KNSU", "KRST", "LAND", "LAVA", "LBED", "LCTY",
            "LDGU", "LDNG", "LEPC", "LEV", "LEVU", "LGN", "LGNS", "LGNX", "LIBR", "LK",
            "LKC", "LKI", "LKN", "LKNI", "LKO", "LKOI", "LKS", "LKSB", "LKSC", "LKSI",
            "LKSN", "LKSNI", "LKX", "LNDF", "LOCK", "LTER", "LTHSE", "MALL", "MAR",
            "MDW", "MESA", "MESU", "MFG", "MFGB", "MFGC", "MFGCU", "MFGLM", "MFGM",
            "MFGN", "MFGPH", "MFGQ", "MFGSG", "MGV", "MILB", "MKT", "ML", "MLM", "MLO",
            "MLSG", "MLSGQ", "MLSW", "MLWND", "MLWTR", "MN", "MNA", "MNAU", "MNC",
            "MNCR", "MNCU", "MND", "MNDU", "MNFE", "MNMT", "MNN", "MNQ", "MNQR",
            "MOLE", "MOOR", "MOTU", "MRN", "MRSH", "MRSHN", "MSQE", "MSSN", "MSSNQ",
            "MSTY", "MT", "MTRO", "MTS", "MTU", "MUS", "MVA", "NKM", "NOV", "NRWS",
            "NSY", "NTK", "NTKS", "NVB", "OAS", "OBPT", "OBS", "OBSR", "OCH", "OCN",
            "OILF", "OILJ", "OILP", "OILQ", "OILR", "OILT", "OILW", "OPRA", "OVF",
            "PAL", "PAN", "PANS", "PASS", "PCL", "PCLD", "PCLF", "PCLH", "PCLI",
            "PCLIX", "PCLS", "PEAT", "PEN", "PENX", "PGDA", "PIER", "PK", "PKLT",
            "PKS", "PKSU", "PKU", "PLAT", "PLATX", "PLDR", "PLN", "PLNU", "PLNX",
            "PLTU", "PMPO", "PMPW", "PND", "PNDI", "PNDN", "PNDNI", "PNDS", "PNDSF",
            "PNDSI", "PNDSN", "PNLU", "PO", "POOL", "POOLI", "PP", "PPL", "PPLA",
            "PPLA2", "PPLA3", "PPLA4", "PPLA5", "PPLC", "PPLCD", "PPLCH", "PPLF",
            "PPLG", "PPLH", "PPLL", "PPLQ", "PPLR", "PPLS", "PPLW", "PPLX", "PPQ",
            "PRK", "PRKGT", "PRKHQ", "PRMN", "PRN", "PRNJ", "PRNQ", "PROM", "PRSH",
            "PRT", "PRVU", "PS", "PSH", "PSN", "PSTB", "PSTC", "PSTP", "PT", "PTGE",
            "PTS", "PYR", "PYRS", "QCKS", "QUAY", "RCH", "RD", "RDA", "RDB", "RDCR",
            "RDCUT", "RDGB", "RDGE", "RDGG", "RDGU", "RDIN", "RDJCT", "RDST", "RDSU",
            "RECG", "RECR", "REG", "RES", "RESA", "RESF", "RESH", "RESN", "RESP",
            "REST", "RESV", "RESW", "RET", "RF", "RFC", "RFSU", "RFU", "RFX", "RGN",
            "RGNE", "RGNH", "RGNL", "RHSE", "RISU", "RJCT", "RK", "RKFL", "RKRY",
            "RKS", "RLG", "RLGR", "RNCH", "RNGA", "RPDS", "RR", "RRQ", "RSD", "RSGNL",
            "RSRT", "RSTN", "RSTNQ", "RSTP", "RSTPQ", "RSV", "RSVI", "RSVT", "RTE",
            "RUIN", "RVN", "RYD", "SALT", "SAND", "SBED", "SBKH", "SCH", "SCHA",
            "SCHC", "SCHL", "SCHM", "SCHN", "SCHT", "SCNU", "SCRB", "SCRP", "SCSU",
            "SD", "SDL", "SDLU", "SEA", "SECP", "SHFU", "SHLU", "SHOL", "SHOR", "SHPF",
            "SHRN", "SHSE", "SHSU", "SHVU", "SILL", "SILU", "SINK", "SLCE", "SLID",
            "SLP", "SLPU", "SMSU", "SMU", "SNOW", "SNTR", "SPA", "SPIT", "SPLY",
            "SPNG", "SPNS", "SPNT", "SPRU", "SPUR", "SQR", "ST", "STBL", "STDM",
            "STKR", "STLMT", "STM", "STMA", "STMB", "STMC", "STMD", "STMH", "STMI",
            "STMIX", "STMM", "STMQ", "STMS", "STMSB", "STMX", "STNB", "STNC", "STNE",
            "STNF", "STNI", "STNM", "STNR", "STNS", "STNW", "STPS", "STRT", "SWMP",
            "SWT", "SYG", "SYSI", "TAL", "TERR", "TERU", "THTR", "TMB", "TMPL", "TMSU",
            "TMTU", "TNGU", "TNKD", "TNL", "TNLC", "TNLN", "TNLRD", "TNLRR", "TNLS",
            "TOLL", "TOWR", "TRAM", "TRANT", "TRB", "TREE", "TRGD", "TRGU", "TRIG",
            "TRL", "TRMO", "TRNU", "TRR", "TUND", "TWO", "UNIP", "UNIV", "UPLD",
            "USGE", "VAL", "VALG", "VALS", "VALU", "VALX", "VETF", "VIN", "VINS",
            "VLC", "VLSU", "WAD", "WADB", "WADJ", "WADM", "WADS", "WADX", "WALL",
            "WALLA", "WEIR", "WHRF", "WHRL", "WLL", "WLLQ", "WLLS", "WRCK", "WTLD",
            "WTLDI", "WTRC", "WTRH", "WTRW", "ZN", "ZNB", "ZNF", "ZOO" };
}
