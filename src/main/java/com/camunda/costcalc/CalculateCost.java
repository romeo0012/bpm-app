package com.camunda.costcalc;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("calculateCost")
public class CalculateCost implements JavaDelegate {

    private static final Logger LOGGER = Logger.getLogger("PROJECT-REQUESTS");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NumberFormat formatter = new DecimalFormat("#,###,###.##");

    private JsonNode cfg;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Calc Start...");

        String configJson = (String) execution.getVariable("configJson");
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalStateException("Process variable 'configJson' is missing or empty");
        }

        cfg = objectMapper.readTree(configJson);

        pushJsonToExecution(cfg, execution);

        double tmp;

        double capex = 0;
        double capexVAT = 0;
        double capexCZVAT = 0;
        double capex1 = 0;
        double capex2 = 0;
        double capex3 = 0;
        double capex1e = 0;
        double capex2e = 0;
        double capex3e = 0;
        double capex1i = 0;
        double capex2i = 0;
        double capex3i = 0;
        double capexCZ = 0;
        double capex1CZ = 0;
        double capex2CZ = 0;
        double capex3CZ = 0;
        double capex1eCZ = 0;
        double capex2eCZ = 0;
        double capex3eCZ = 0;
        double capex1iCZ = 0;
        double capex2iCZ = 0;
        double capex3iCZ = 0;

        tmp = (l("dvip") + l("tvip") + l("qvip") + l("bvip") + l("pvip")) * d("VipDeployCost");
        capex1 += tmp;
        if (b("VipIsExt")) { capex1e += tmp; } else { capex1i += tmp; }

        tmp = (l("dnat") + l("tnat") + l("qnat") + l("bnat") + l("pnat")) * d("NatDeployCost");
        capex1 += tmp;
        if (b("NatIsExt")) { capex1e += tmp; } else { capex1i += tmp; }

        tmp = (l("dsrv") + l("tsrv") + l("qsrv") + l("bsrv") + l("psrv")) * d("SrvDeployCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = (l("ddb") + l("tdb") + l("qdb") + l("bdb") + l("pdb")) * d("DbDeployCost");
        capex1 += tmp;
        if (b("DbIsExt")) { capex1e += tmp; } else { capex1i += tmp; }

        tmp = (l("dcerp") + l("tcerp") + l("qcerp") + l("bcerp") + l("pcerp")) * d("PublicCertCost");
        tmp += (l("dcerp") + l("tcerp") + l("qcerp") + l("bcerp") + l("pcerp")) * d("CertDeployCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = (l("dceri") + l("tceri") + l("qceri") + l("bceri") + l("pceri")) * d("InternalCertCost");
        tmp += (l("dceri") + l("tceri") + l("qceri") + l("bceri") + l("pceri")) * d("CertDeployCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = (l("dftq") + l("tftq") + l("qftq") + l("bftq") + l("pftq")) * d("FtqDeployCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = (l("dmsq") + l("tmsq") + l("qmsq") + l("bmsq") + l("pmsq")) * d("MsqDeployCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = l("vpn1") * d("IndividualVpnCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = 0;
        if (b("WebFirewall")) tmp += d("FirewallRuleCost");
        if (b("ApplicationFirewall")) tmp += d("FirewallRuleCost");
        if (b("MiddlewareFirewall")) tmp += d("FirewallRuleCost");
        if (b("DatabaseFirewall")) tmp += d("FirewallRuleCost");
        if (b("WebApacheReverseProxy")) tmp += d("ApacheReverseProxyCost");
        if (b("ApplicationApacheReverseProxy")) tmp += d("ApacheReverseProxyCost");
        if (b("MiddlewareApacheReverseProxy")) tmp += d("ApacheReverseProxyCost");
        if (b("ApplicationServerActiveActive")) tmp += d("ApplicationServerActiveActiveCost");
        if (b("MiddlewareServerActiveActive")) tmp += d("ApplicationServerActiveActiveCost");
        if (b("WebEMCAutostart")) tmp += d("EMCAutostartCost");
        if (b("ApplicationEMCAutostart")) tmp += d("EMCAutostartCost");
        if (b("MiddlewareEMCAutostart")) tmp += d("EMCAutostartCost");
        if (b("DatabaseEMCAutostart")) tmp += d("EMCAutostartCost");
        if (b("WebVMwareHA")) tmp += d("VMwareHACost");
        if (b("ApplicationVMwareHA")) tmp += d("VMwareHACost");
        if (b("MiddlewareVMwareHA")) tmp += d("VMwareHACost");
        if (b("DatabaseVMwareHA")) tmp += d("VMwareHACost");
        if (b("OracleDataGuard")) tmp += d("OracleDataGuardCost");
        if (b("MSSQLCluster")) tmp += d("MSSQLClusterCost");
        if (b("ApplicationMSWindowsCluster")) tmp += d("MSWindowsClusterCost");
        if (b("MiddlewareMSWindowsCluster")) tmp += d("MSWindowsClusterCost");
        if (b("WebRedHatCluster")) tmp += d("RedHatClusterCost");
        if (b("ApplicationRedHatCluster")) tmp += d("RedHatClusterCost");
        if (b("MiddlewareRedHatCluster")) tmp += d("RedHatClusterCost");
        if (b("ApplicationRedisSentinelCluster")) tmp += d("RedisSentinelClusterCost");
        if (b("MiddlewareRedisSentinelCluster")) tmp += d("RedisSentinelClusterCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = (l("tdes1") + l("tdes2") + l("tdes3") + l("tdes4") + l("tdes5") + l("tdes6")) * d("DesignCost");
        capex1 += tmp;
        capex1i += tmp;

        tmp = (l("pdes1") + l("pdes2") + l("pdes3") + l("pdes4") + l("pdes5") + l("pdes6")) * d("DesignCost");
        capex1 += tmp;
        capex1i += tmp;

        LOGGER.info("SLA = " + s("sla"));
        LOGGER.info("CAPEX1.0 = " + capex1);
        LOGGER.info("Sla8x5costRatio = " + d("Sla8x5costRatio"));
        LOGGER.info("Sla24x7costRatio = " + d("Sla24x7costRatio"));
        LOGGER.info("IctPassiveCostRatio = " + d("IctPassiveCostRatio"));
        LOGGER.info("IctActiveCostRatio = " + d("IctActiveCostRatio"));

        if (s("sla").contains("8x5")) { capex1 = capex1 * d("Sla8x5costRatio") / 100 + capex1; }
        if (s("sla").contains("24x7")) { capex1 = capex1 * d("Sla24x7costRatio") / 100 + capex1; }
        if (s("continuity").contains("passive")) { capex1 = capex1 * d("IctPassiveCostRatio") / 100 + capex1; }
        if (s("continuity").contains("active")) { capex1 = capex1 * d("IctActiveCostRatio") / 100 + capex1; }

        if (s("sla").contains("8x5")) { capex1i = capex1i * d("Sla8x5costRatio") / 100 + capex1i; }
        if (s("sla").contains("24x7")) { capex1i = capex1i * d("Sla24x7costRatio") / 100 + capex1i; }
        if (s("continuity").contains("passive")) { capex1i = capex1i * d("IctPassiveCostRatio") / 100 + capex1i; }
        if (s("continuity").contains("active")) { capex1i = capex1i * d("IctActiveCostRatio") / 100 + capex1i; }

        if (s("sla").contains("8x5")) { capex1e = capex1e * d("Sla8x5costRatio") / 100 + capex1e; }
        if (s("sla").contains("24x7")) { capex1e = capex1e * d("Sla24x7costRatio") / 100 + capex1e; }
        if (s("continuity").contains("passive")) { capex1e = capex1e * d("IctPassiveCostRatio") / 100 + capex1e; }
        if (s("continuity").contains("active")) { capex1e = capex1e * d("IctActiveCostRatio") / 100 + capex1e; }

        tmp = l("prg1") * d("ProgramingCost");
        capex1 += tmp;
        if (b("PrgIsExt")) { capex1e += tmp; } else { capex1i += tmp; }

        tmp = l("dpl1") * d("AppDeploymentCost");
        capex1 += tmp;
        if (b("DplIsExt")) { capex1e += tmp; } else { capex1i += tmp; }

        tmp = l("prg2") * d("ProgramingCost");
        capex2 += tmp;
        if (b("PrgIsExt")) { capex2e += tmp; } else { capex2i += tmp; }

        tmp = l("dpl2") * d("AppDeploymentCost");
        capex2 += tmp;
        if (b("DplIsExt")) { capex2e += tmp; } else { capex2i += tmp; }

        tmp = l("prg3") * d("ProgramingCost");
        capex3 += tmp;
        if (b("PrgIsExt")) { capex3e += tmp; } else { capex3i += tmp; }

        tmp = l("dpl3") * d("AppDeploymentCost");
        capex3 += tmp;
        if (b("DplIsExt")) { capex3e += tmp; } else { capex3i += tmp; }

        capex = capex1 + capex2 + capex3;
        capexVAT = capex * (100 + d("VAT")) / 100;

        execution.setVariable("capex1e", formatter.format(capex1e));
        execution.setVariable("capex2e", formatter.format(capex2e));
        execution.setVariable("capex3e", formatter.format(capex3e));
        execution.setVariable("capex1i", formatter.format(capex1i));
        execution.setVariable("capex2i", formatter.format(capex2i));
        execution.setVariable("capex3i", formatter.format(capex3i));
        execution.setVariable("capex1", formatter.format(capex1));
        execution.setVariable("capex2", formatter.format(capex2));
        execution.setVariable("capex3", formatter.format(capex3));
        execution.setVariable("capex", formatter.format(capex));
        execution.setVariable("capexVAT", formatter.format(capexVAT));

        double opex = 0;
        double opexVAT = 0;
        double opexCZVAT = 0;
        double opex1 = 0;
        double opex2 = 0;
        double opex3 = 0;
        double capexopex = 0;
        double capexopexVAT = 0;
        double opexCZ = 0;
        double opex1CZ = 0;
        double opex2CZ = 0;
        double opex3CZ = 0;
        double capexopexCZ = 0;
        double capexopexCZVAT = 0;

        opex1 += (d("ServerCost") * l("cpu1") / d("VmRatio") / d("CpuBlock")) + (d("StorageCost") * l("hdd1") / d("StorageBlock")) + (d("StorageCost") * l("database1") / d("StorageBlock"));
        opex2 += (d("ServerCost") * l("cpu2") / d("VmRatio") / d("CpuBlock")) + (d("StorageCost") * l("hdd2") / d("StorageBlock")) + (d("StorageCost") * l("database2") / d("StorageBlock"));
        opex3 += (d("ServerCost") * l("cpu3") / d("VmRatio") / d("CpuBlock")) + (d("StorageCost") * l("hdd3") / d("StorageBlock")) + (d("StorageCost") * l("database3") / d("StorageBlock"));

        opex1 += (l("lic1") * d("LicenseCost")) + (l("mtn1") * d("MaintanaceCost"));
        opex2 += (l("lic2") * d("LicenseCost")) + (l("mtn2") * d("MaintanaceCost"));
        opex3 += (l("lic3") * d("LicenseCost")) + (l("mtn3") * d("MaintanaceCost"));

        double certs = l("dcerp") + l("tcerp") + l("qcerp") + l("bcerp") + l("pcerp")
                + l("dceri") + l("tceri") + l("qceri") + l("bceri") + l("pceri");

        opex1 += certs * d("PkiMgmtCost");
        opex2 += certs * d("PkiMgmtCost");
        opex3 += certs * d("PkiMgmtCost");

        opex1 += d("LeasedLineCostPerMbps") * l("llb1");
        opex2 += d("LeasedLineCostPerMbps") * l("llb2");
        opex3 += d("LeasedLineCostPerMbps") * l("llb3");

        if (l("ssvpn1") > 0) { opex1 += l("ssvpn1") * d("Site2SsiteVpnCostPerMbps"); }
        if (l("ssvpn2") > 0) { opex2 += l("ssvpn2") * d("Site2SsiteVpnCostPerMbps"); }
        if (l("ssvpn3") > 0) { opex3 += l("ssvpn3") * d("Site2SsiteVpnCostPerMbps"); }
        if (l("llb1") > 0) { opex1 = l("llb1") * d("LeasedLineCostPerMbps"); }
        if (l("llb2") > 0) { opex2 = l("llb2") * d("LeasedLineCostPerMbps"); }
        if (l("llb3") > 0) { opex3 = l("llb3") * d("LeasedLineCostPerMbps"); }

        if (l("trs1") > 0) { opex1 += l("trs1") * d("TransactionCost"); }
        if (l("trs2") > 0) { opex2 += l("trs2") * d("TransactionCost"); }
        if (l("trs3") > 0) { opex3 += l("trs3") * d("TransactionCost"); }
        if (b("buslog1")) { opex1 += d("BusLogCost"); }
        if (b("buslog2")) { opex2 += d("BusLogCost"); }
        if (b("buslog3")) { opex3 += d("BusLogCost"); }
        if (b("techlog1")) { opex1 += d("TechLogCost"); }
        if (b("techlog2")) { opex2 += d("TechLogCost"); }
        if (b("techlog3")) { opex3 += d("TechLogCost"); }

        if (b("NurInt")) { opex1 += d("NurIntCost"); opex2 += d("NurIntCost"); opex3 += d("NurIntCost"); }
        if (b("NurWin")) { opex1 += d("NurWinCost"); opex2 += d("NurWinCost"); opex3 += d("NurWinCost"); }
        if (b("NurSia")) { opex1 += d("NurSiaCost"); opex2 += d("NurSiaCost"); opex3 += d("NurSiaCost"); }
        if (b("NurFis")) { opex1 += d("NurFisCost"); opex2 += d("NurFisCost"); opex3 += d("NurFisCost"); }
        if (b("NurWan")) { opex1 += d("NurWanCost"); opex2 += d("NurWanCost"); opex3 += d("NurWanCost"); }
        if (b("NurPrgDcMpls")) { opex1 += d("NurPrgDcMplsCost"); opex2 += d("NurPrgDcMplsCost"); opex3 += d("NurPrgDcMplsCost"); }
        if (b("PrgInt")) { opex1 += d("PrgIntCost"); opex2 += d("PrgIntCost"); opex3 += d("PrgIntCost"); }
        if (b("PrgGpe")) { opex1 += d("PrgGpeCost"); opex2 += d("PrgGpeCost"); opex3 += d("PrgGpeCost"); }
        if (b("PrgSia")) { opex1 += d("PrgSiaCost"); opex2 += d("PrgSiaCost"); opex3 += d("PrgSiaCost"); }

        opex = opex1 + opex2 + opex3;
        opexVAT = opex * (100 + d("VAT")) / 100;

        execution.setVariable("opex1", formatter.format(opex1));
        execution.setVariable("opex2", formatter.format(opex2));
        execution.setVariable("opex3", formatter.format(opex3));
        execution.setVariable("opex", formatter.format(opex));
        execution.setVariable("opexVAT", formatter.format(opexVAT));

        capexopex = capex + opex;
        capexopexVAT = (capex + opex) * (100 + d("VAT")) / 100;

        execution.setVariable("capexopex", formatter.format(capexopex));
        execution.setVariable("capexopexVAT", formatter.format(capexopexVAT));

        capex1eCZ = capex1e * d("Eur2Czk");
        capex2eCZ = capex2e * d("Eur2Czk");
        capex3eCZ = capex3e * d("Eur2Czk");
        capex1iCZ = capex1i * d("Eur2Czk");
        capex2iCZ = capex2i * d("Eur2Czk");
        capex3iCZ = capex3i * d("Eur2Czk");
        capexCZ = capex * d("Eur2Czk");
        capex1CZ = capexCZ;
        capex2CZ = 0;
        capex3CZ = 0;
        capexCZVAT = capexCZ * (100 + d("VAT")) / 100;

        execution.setVariable("capex1eCZ", formatter.format(capex1eCZ));
        execution.setVariable("capex2eCZ", formatter.format(capex2eCZ));
        execution.setVariable("capex3eCZ", formatter.format(capex3eCZ));
        execution.setVariable("capex1iCZ", formatter.format(capex1iCZ));
        execution.setVariable("capex2iCZ", formatter.format(capex2iCZ));
        execution.setVariable("capex3iCZ", formatter.format(capex3iCZ));
        execution.setVariable("capex1CZ", formatter.format(capex1CZ));
        execution.setVariable("capex2CZ", formatter.format(capex2CZ));
        execution.setVariable("capex3CZ", formatter.format(capex3CZ));
        execution.setVariable("capexCZ", formatter.format(capexCZ));
        execution.setVariable("capexCZVAT", formatter.format(capexCZVAT));

        opex1CZ = opex1 * d("Eur2Czk");
        opex2CZ = opex2 * d("Eur2Czk");
        opex3CZ = opex3 * d("Eur2Czk");
        opexCZ = opex * d("Eur2Czk");
        opexCZVAT = opexCZ * (100 + d("VAT")) / 100;

        execution.setVariable("opex1CZ", formatter.format(opex1CZ));
        execution.setVariable("opex2CZ", formatter.format(opex2CZ));
        execution.setVariable("opex3CZ", formatter.format(opex3CZ));
        execution.setVariable("opexCZ", formatter.format(opexCZ));
        execution.setVariable("opexCZVAT", formatter.format(opexCZVAT));

        capexopexCZ = capexCZ + opexCZ;
        capexopexCZVAT = (capexCZ + opexCZ) * (100 + d("VAT")) / 100;

        execution.setVariable("capexopexCZ", formatter.format(capexopexCZ));
        execution.setVariable("capexopexCZVAT", formatter.format(capexopexCZVAT));

        LOGGER.info("Calc Complete");
    }

    private void pushJsonToExecution(JsonNode json, DelegateExecution execution) {
    Iterator<Entry<String, JsonNode>> properties = json.properties().iterator(); // ✅ Set → Iterator
    while (properties.hasNext()) {
            Entry<String, JsonNode> field = properties.next();
            JsonNode value = field.getValue();

            if (value.isIntegralNumber()) {
                execution.setVariable(field.getKey(), value.asLong());
            } else if (value.isFloatingPointNumber()) {
                execution.setVariable(field.getKey(), value.asDouble());
            } else if (value.isBoolean()) {
                execution.setVariable(field.getKey(), value.asBoolean());
            } else if (value.isTextual()) {
                execution.setVariable(field.getKey(), value.asText());
            }
        }
    }

    private long l(String key) {
        return cfg.path(key).asLong(0L);
    }

    private double d(String key) {
        return cfg.path(key).asDouble(0D);
    }

    private boolean b(String key) {
        return cfg.path(key).asBoolean(false);
    }

    private String s(String key) {
        return cfg.path(key).asText("");
    }
}