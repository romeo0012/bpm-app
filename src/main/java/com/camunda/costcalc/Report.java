package com.camunda.costcalc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;

@Component("report")
public class Report implements JavaDelegate {

    private static final Logger LOGGER = Logger.getLogger("PROJECT-REQUESTS");

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        try (Workbook workbook = new XSSFWorkbook()) {

            DataFormat format = workbook.createDataFormat();
            Sheet sheet;
            Row row;
            Cell cell;

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle headerStyle2 = workbook.createCellStyle();
            headerStyle2.setFont(headerFont);
            headerStyle2.setAlignment(HorizontalAlignment.CENTER);
            headerStyle2.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
            headerStyle2.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(false);
            labelStyle.setFont(labelFont);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat(format.getFormat("#,##0"));
            cellStyle.setAlignment(HorizontalAlignment.CENTER);

            Map<String, Object[]> data;
            Set<String> keyid;
            int rowid;
            int headerrow;
            int cellid;
            int rowpointer = 0;
            int tablerows;

            String reportName = s(execution, "ReportName", "CostCalc");
            String reportDir = s(execution, "ReportDir", "./");

            sheet = workbook.createSheet(reportName + " Costs");
            sheet.setColumnWidth(0, 9000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 5000);
            sheet.setColumnWidth(3, 5000);
            sheet.setColumnWidth(4, 5000);
            sheet.setColumnWidth(5, 5000);
            sheet.setColumnWidth(6, 5000);
            sheet.setColumnWidth(7, 5000);

            data = new TreeMap<>();
            data.put("1", new Object[] { "Total costs", "Capex", "Opex", "3y Total" });
            data.put("2", new Object[] { "Total in EUR", d(execution, "capex"), d(execution, "opex"), d(execution, "capexopex") });
            data.put("3", new Object[] { "Total in EUR inc. VAT", d(execution, "capexVAT"), d(execution, "opexVAT"), d(execution, "capexopexVAT") });
            data.put("4", new Object[] { "Total in CZK", d(execution, "capexCZ"), d(execution, "opexCZ"), d(execution, "capexopexCZ") });
            data.put("5", new Object[] { "Total in CZK inc. VAT", d(execution, "capexCZVAT"), d(execution, "opexCZVAT"), d(execution, "capexopexCZVAT") });
            keyid = data.keySet();
            tablerows = 7;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle2);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            writeNumberOrDash(cell, obj, cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "Costs per year", "1st year", "Year + 2", "Year + 3" });
            data.put("2", new Object[] { "Capex in EUR (External)", d(execution, "capex1e"), d(execution, "capex2e"), d(execution, "capex3e") });
            data.put("3", new Object[] { "Capex in EUR (Internal)", d(execution, "capex1i"), d(execution, "capex2i"), d(execution, "capex3i") });
            data.put("4", new Object[] { "Opex in EUR", d(execution, "opex1"), d(execution, "opex2"), d(execution, "opex3") });
            data.put("5", new Object[] { "Capex in CZK (External)", d(execution, "capex1eCZ"), d(execution, "capex2eCZ"), d(execution, "capex3eCZ") });
            data.put("6", new Object[] { "Capex in CZK (Internal)", d(execution, "capex1iCZ"), d(execution, "capex2iCZ"), d(execution, "capex3iCZ") });
            data.put("7", new Object[] { "Opex in CZK", d(execution, "opex1CZ"), d(execution, "opex2CZ"), d(execution, "opex3CZ") });
            keyid = data.keySet();
            tablerows = 9;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle2);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            writeNumberOrDash(cell, obj, cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "SLA / IT Continuity Solution", "RTO in hours", "RPO in min", "Archiv in years" });
            data.put("2", new Object[] { s(execution, "sla", "") + " / " + s(execution, "continuity", ""), l(execution, "rto"), l(execution, "rpo"), l(execution, "archiv") });
            keyid = data.keySet();
            tablerows = 4;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle2);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            writeNumberOrDash(cell, obj, cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "Environments", "External Service", "Development", "Test", "Quality Assurance", "Bug Fix", "Production" });
            data.put("2", new Object[] { "Load Balanced VIP", boolMark(execution, "VipIsExt"), l(execution, "dvip"), l(execution, "tvip"), l(execution, "qvip"), l(execution, "bvip"), l(execution, "pvip") });
            data.put("3", new Object[] { "Firewall NAT", boolMark(execution, "NatIsExt"), l(execution, "dnat"), l(execution, "tnat"), l(execution, "qnat"), l(execution, "bnat"), l(execution, "pnat") });
            data.put("4", new Object[] { "Server", boolMark(execution, "SrvIsExt"), l(execution, "dsrv"), l(execution, "tsrv"), l(execution, "qsrv"), l(execution, "bsrv"), l(execution, "psrv") });
            data.put("5", new Object[] { "Database", boolMark(execution, "DbIsExt"), l(execution, "ddb"), l(execution, "tdb"), l(execution, "qdb"), l(execution, "bdb"), l(execution, "pdb") });
            data.put("6", new Object[] { "Public Commercial Certificate", "-", l(execution, "dcerp"), l(execution, "tcerp"), l(execution, "qcerp"), l(execution, "bcerp"), l(execution, "pcerp") });
            data.put("7", new Object[] { "Internally Issued Certificate", "-", l(execution, "dceri"), l(execution, "tceri"), l(execution, "qceri"), l(execution, "bceri"), l(execution, "pceri") });
            data.put("8", new Object[] { "File Transfer Queues", "-", l(execution, "dftq"), l(execution, "tftq"), l(execution, "qftq"), l(execution, "bftq"), l(execution, "pftq") });
            data.put("9", new Object[] { "Message Queues", "-", l(execution, "dmsq"), l(execution, "tmsq"), l(execution, "qmsq"), l(execution, "bmsq"), l(execution, "pmsq") });
            keyid = data.keySet();
            tablerows = 11;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else if (cellid == 2) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(cellStyle);
                        } else {
                            writeNumberOrDash(cell, obj, cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "Direct Resources", "1st year", "Year + 2", "Year + 3" });
            data.put("2", new Object[] { "CPU Cores", l(execution, "cpu1"), l(execution, "cpu2"), l(execution, "cpu3") });
            data.put("3", new Object[] { "Memory in GB", l(execution, "memory1"), l(execution, "memory2"), l(execution, "memory3") });
            data.put("4", new Object[] { "Server Disk in GB", l(execution, "hdd1"), l(execution, "hdd2"), l(execution, "hdd3") });
            data.put("5", new Object[] { "Database Size in GB", l(execution, "database1"), l(execution, "database2"), l(execution, "database3") });
            data.put("6", new Object[] { "Individual VPN accounts", l(execution, "vpn1"), l(execution, "vpn2"), l(execution, "vpn3") });
            data.put("7", new Object[] { "Site-to-Site VPN bandwidth in Mbps", l(execution, "ssvpn1"), l(execution, "ssvpn2"), l(execution, "ssvpn3") });
            data.put("8", new Object[] { "Leased Line bandwidth in Mbps", l(execution, "llb1"), l(execution, "llb2"), l(execution, "llb3") });
            keyid = data.keySet();
            tablerows = 10;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            writeNumberOrDash(cell, obj, cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "Support Resources", "1st year", "Year + 2", "Year + 3" });
            data.put("2", new Object[] { "Transactions store in GB", l(execution, "trs1"), l(execution, "trs2"), l(execution, "trs3") });
            data.put("3", new Object[] { "Business logging", boolMark(execution, "buslog1"), boolMark(execution, "buslog2"), boolMark(execution, "buslog3") });
            data.put("4", new Object[] { "Technical logging", boolMark(execution, "techlog1"), boolMark(execution, "techlog2"), boolMark(execution, "techlog3") });
            keyid = data.keySet();
            tablerows = 6;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "Network lines", "1st year", "Year + 2", "Year + 3" });
            data.put("2", new Object[] { "PRG - Internet", boolMark(execution, "PrgInt"), boolMark(execution, "PrgInt"), boolMark(execution, "PrgInt") });
            data.put("3", new Object[] { "PRG - GPE", boolMark(execution, "PrgGpe"), boolMark(execution, "PrgGpe"), boolMark(execution, "PrgGpe") });
            data.put("4", new Object[] { "PRG - SIA CREDIT", boolMark(execution, "PrgSia"), boolMark(execution, "PrgSia"), boolMark(execution, "PrgSia") });
            data.put("5", new Object[] { "PRG Office - NUR", boolMark(execution, "NurPrgDcMpls"), boolMark(execution, "NurPrgDcMpls"), boolMark(execution, "NurPrgDcMpls") });
            data.put("6", new Object[] { "NUR - Internet", boolMark(execution, "NurInt"), boolMark(execution, "NurInt"), boolMark(execution, "NurInt") });
            data.put("7", new Object[] { "NUR - ITG Win", boolMark(execution, "NurWin"), boolMark(execution, "NurWin"), boolMark(execution, "NurWin") });
            data.put("8", new Object[] { "NUR - SIA DEBET", boolMark(execution, "NurSia"), boolMark(execution, "NurSia"), boolMark(execution, "NurSia") });
            data.put("9", new Object[] { "NUR - FIS", boolMark(execution, "NurFis"), boolMark(execution, "NurFis"), boolMark(execution, "NurFis") });
            data.put("10", new Object[] { "NUR - BNP WAN", boolMark(execution, "NurWan"), boolMark(execution, "NurWan"), boolMark(execution, "NurWan") });
            keyid = data.keySet();
            tablerows = 12;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "High availability", "Public Web Zone", "Application Zone", "Middleware Zone", "Database Zone" });
            data.put("2", new Object[] { "Firewall Rule or NAT", boolMark(execution, "WebFirewall"), boolMark(execution, "ApplicationFirewall"), boolMark(execution, "MiddlewareFirewall"), boolMark(execution, "DatabaseFirewall") });
            data.put("3", new Object[] { "Apache Reverse Proxy", boolMark(execution, "WebApacheReverseProxy"), boolMark(execution, "ApplicationApacheReverseProxy"), boolMark(execution, "MiddlewareApacheReverseProxy"), "-" });
            data.put("4", new Object[] { "Application Server Active Active", "-", boolMark(execution, "ApplicationServerActiveActive"), boolMark(execution, "MiddlewareServerActiveActive"), "-" });
            data.put("5", new Object[] { "EMC Autostart", boolMark(execution, "WebEMCAutostart"), boolMark(execution, "ApplicationEMCAutostart"), boolMark(execution, "MiddlewareEMCAutostart"), boolMark(execution, "DatabaseEMCAutostart") });
            data.put("6", new Object[] { "VMware HA", boolMark(execution, "WebVMwareHA"), boolMark(execution, "ApplicationVMwareHA"), boolMark(execution, "MiddlewareVMwareHA"), boolMark(execution, "DatabaseVMwareHA") });
            data.put("7", new Object[] { "Oracle Data Guard", "-", "-", "-", boolMark(execution, "OracleDataGuard") });
            data.put("8", new Object[] { "MS SQL Cluster", "-", "-", "-", boolMark(execution, "MSSQLCluster") });
            data.put("9", new Object[] { "MS Windows Cluster", "-", boolMark(execution, "ApplicationMSWindowsCluster"), boolMark(execution, "MiddlewareMSWindowsCluster"), "-" });
            data.put("10", new Object[] { "Red Hat Cluster", boolMark(execution, "WebRedHatCluster"), boolMark(execution, "ApplicationRedHatCluster"), boolMark(execution, "MiddlewareRedHatCluster"), "-" });
            data.put("11", new Object[] { "Redis Sentinel Cluster", "-", boolMark(execution, "ApplicationRedisSentinelCluster"), boolMark(execution, "MiddlewareRedisSentinelCluster"), "-" });
            keyid = data.keySet();
            tablerows = 13;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
            }

            data = new TreeMap<>();
            data.put("1", new Object[] { "Infrastructure", "External Service", "PSD", "DEV", "TST", "QA", "BFX", "PROD" });
            data.put("2", new Object[] { "Technical Design (MD)", "-", l(execution, "tdes1"), l(execution, "tdes2"), l(execution, "tdes3"), l(execution, "tdes4"), l(execution, "tdes5"), l(execution, "tdes6") });
            data.put("3", new Object[] { "Production Design (MD)", "-", l(execution, "pdes1"), l(execution, "pdes2"), l(execution, "pdes3"), l(execution, "pdes4"), l(execution, "pdes5"), l(execution, "pdes6") });
            data.put("4", new Object[] { "Programming (MD)", boolMark(execution, "PrgIsExt"), l(execution, "prg1"), l(execution, "prg2"), l(execution, "prg3"), l(execution, "prg4"), l(execution, "prg5"), l(execution, "prg6") });
            data.put("5", new Object[] { "Deployment (MD)", boolMark(execution, "DplIsExt"), l(execution, "dpl1"), l(execution, "dpl2"), l(execution, "dpl3"), l(execution, "dpl4"), l(execution, "dpl5"), l(execution, "dpl6") });
            data.put("6", new Object[] { "License (EUR/y)", "-", l(execution, "lic1"), l(execution, "lic2"), l(execution, "lic3"), l(execution, "lic4"), l(execution, "lic5"), l(execution, "lic6") });
            data.put("7", new Object[] { "Maintenance (MD/y)", "-", l(execution, "mtn1"), l(execution, "mtn2"), l(execution, "mtn3"), l(execution, "mtn4"), l(execution, "mtn5"), l(execution, "mtn6") });
            keyid = data.keySet();
            tablerows = 9;
            rowid = rowpointer;
            rowpointer += tablerows;
            headerrow = rowid + 1;
            for (String key : keyid) {
                row = sheet.createRow(rowid++);
                Object[] objectArr = data.get(key);
                cellid = 0;
                for (Object obj : objectArr) {
                    cell = row.createCell(cellid++);
                    if (rowid == headerrow) {
                        cell.setCellValue(String.valueOf(obj));
                        cell.setCellStyle(headerStyle);
                    } else {
                        if (cellid == 1) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(labelStyle);
                        } else if (cellid == 2) {
                            cell.setCellValue(String.valueOf(obj));
                            cell.setCellStyle(cellStyle);
                        } else {
                            writeNumberOrDash(cell, obj, cellStyle);
                        }
                    }
                }
            }

            File dir = new File(reportDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileLocation = reportDir + File.separator + reportName + ".xlsx";

            try (FileOutputStream outputStream = new FileOutputStream(fileLocation)) {
                workbook.write(outputStream);
            }

            execution.setVariable("EXCEL_REPORT", Variables.fileValue(reportName + ".xlsx").file(new File(fileLocation)).create());

            LOGGER.info("Report written: " + fileLocation);
        }
    }

    private void writeNumberOrDash(Cell cell, Object value, CellStyle style) {
        cell.setCellStyle(style);

        if (value == null) {
            cell.setCellValue("-");
            return;
        }

        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == 0) {
                cell.setCellValue("-");
            } else {
                cell.setCellValue(d);
            }
            return;
        }

        String s = String.valueOf(value);
        if ("-".equals(s) || s.isBlank()) {
            cell.setCellValue("-");
            return;
        }

        try {
            double d = Double.parseDouble(s.replace(",", ""));
            if (d == 0) {
                cell.setCellValue("-");
            } else {
                cell.setCellValue(d);
            }
        } catch (Exception e) {
            cell.setCellValue(s);
        }
    }

    private String boolMark(DelegateExecution execution, String key) {
        Object value = execution.getVariable(key);
        if (value == null) {
            return "-";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "√" : "-";
        }
        return String.valueOf(value).equalsIgnoreCase("true") ? "√" : "-";
    }

    private String s(DelegateExecution execution, String key, String defaultValue) {
        Object value = execution.getVariable(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private long l(DelegateExecution execution, String key) {
        Object value = execution.getVariable(key);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value).replace(",", ""));
        } catch (Exception e) {
            return 0L;
        }
    }

    private double d(DelegateExecution execution, String key) {
        Object value = execution.getVariable(key);
        if (value == null) return 0D;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value).replace(",", ""));
        } catch (Exception e) {
            return 0D;
        }
    }
}