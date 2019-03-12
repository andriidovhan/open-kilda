/* Copyright 2019 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.grpc.speaker.model;

import java.util.Arrays;

public enum ErrorCode {

    ERRNO_1(1, "Error occurred."),
    ERRNO_2(2, "System error."),
    ERRNO_3(3, "System error."),
    ERRNO_4(4, "System error."),
    ERRNO_6(6, "Incomplete command. See 'help' or hit \"tab\" key twice for auto-completion options."),
    ERRNO_7(7, "Too many arguments for a command."),
    ERRNO_8(8, "There are at least two redundant arguments."),
    ERRNO_9(9, "Command length is too long."),
    ERRNO_10(10, "System error."),
    ERRNO_11(11, "Command is reserved for NoviFlow Support."),
    ERRNO_12(12, "Unrecognized non-ASCII character."),
    ERRNO_13(13, "Invalid parameter. Please see 'help' command for supported parameters."),
    ERRNO_14(14, "Parameter value provided is invalid."),
    ERRNO_15(15, "Invalid command entered."),
    ERRNO_16(16, "Command not yet supported."),
    ERRNO_17(17, "Config value is invalid. Expected format is 'on'."),
    ERRNO_18(18, "Config value is invalid. Expected format is 'off'."),
    ERRNO_19(19, "Config value is invalid. Expected format is 'on' or 'off'."),
    ERRNO_20(20, "Could not recognize interface."),
    ERRNO_21(21, "Unable to delete route."),
    ERRNO_22(22, "Unable to set route."),
    ERRNO_23(23, "IP address is not valid."),
    ERRNO_24(24, "Mask is not valid."),
    ERRNO_25(25, "Unable to get the ip of the interface."),
    ERRNO_26(26, "Unable to get the mask of the interface."),
    ERRNO_27(27, "Invalid IPv4 address."),
    ERRNO_29(29, "Invalid Netmask value."),
    ERRNO_30(30, "Bad combination of IP and Netmask."),
    ERRNO_31(31, "Invalid portno value."),
    ERRNO_32(32, "Interface provided is invalid."),
    ERRNO_33(33, "Could not configure specified VLAN."),
    ERRNO_34(34, "Specified VLAN does not exist."),
    ERRNO_35(35, "Invalid vlan_id value. Expected format is an integer between 2 and 4094."),
    ERRNO_36(36, "System error: Could not open the file."),
    ERRNO_37(37, "File name is not valid."),
    ERRNO_39(39, "System error: socket is not connected."),
    ERRNO_40(40, "System error: read failure."),
    ERRNO_41(41, "System error: write failure."),
    ERRNO_42(42, "Date is invalid. Verify that the year value is right."),
    ERRNO_43(43, "Date format is invalid. Expected format is DD[/MM[/YYYY]]."),
    ERRNO_44(44, "Time format is invalid. Expected format is HH[:MM[:SS]]."),
    ERRNO_45(45, "Timezone format is invalid. Expected format is UTCÂ±HH[:MM]. Valid values for MM are 0 or 30."),
    ERRNO_46(46, "This timezone does not exist."),
    ERRNO_47(47, "Timezone is not valid."),
    ERRNO_48(48, "Username is reserved."),
    ERRNO_49(49, "Format is invalid. Expecting: username <username> password <password> group <admin/monitoring>."),
    ERRNO_50(50, "User does not have Privilege"),
    ERRNO_51(51, "Only superuser can modify superuser or admin accounts."),
    ERRNO_52(52, "This command is reserved to admins."),
    ERRNO_53(53, "Could not find user."),
    ERRNO_54(54, "Only superuser password can be changed. Please delete and create new accounts for other users."),
    ERRNO_55(55, "This account does not have the privileges to delete target user."),
    ERRNO_56(56, "User already exists."),
    ERRNO_57(57, "No user found."),
    ERRNO_58(58, "Password is not valid."),
    ERRNO_59(59, "Unable to delete user."),
    ERRNO_60(60, "Invalid Echo interval. Expected value is an integer between 0 and 255."),
    ERRNO_61(61, "Matchfields format is invalid. Please see 'help' command for supported matchfields."),
    ERRNO_62(62, "OFPXMT_OFB_METADATA cannot be added to table 0"),
    ERRNO_63(63, "Maximum size of matchfield exceeded."),
    ERRNO_64(64, "Build provided is invalid. Expected 'previous'."),
    ERRNO_65(65, "RPM not found."),
    ERRNO_66(66, "TFTP Download Failed, please Check the Path/filename or IP address"),
    ERRNO_67(67, "Upload failed due to TFTP server timeout."),
    ERRNO_68(68, "NoviWare Upgrade file not found, please upload new package first."),
    ERRNO_69(69, "NoviWare upgrade file not found"),
    ERRNO_70(70, "Could not extract NoviWare package."),
    ERRNO_71(71, "NoviWare Package is invalid"),
    ERRNO_72(72, "Could not finish NoviWare upgrade."),
    ERRNO_73(73, "System error: upgrade could not be completed."),
    ERRNO_74(74, "System error: permission denied."),
    ERRNO_75(75, "Could not create backup during NoviWare upgrade."),
    ERRNO_76(76, "System error: upgrade could not be completed."),
    ERRNO_77(77, "Could not launch NoviWare, old build reverted."),
    ERRNO_81(81, "MissSendLen format is invalid. Expected format is an integer between 0 and 9216, "
            + "or the value 65535."),
    ERRNO_82(82, "Datapath ID format is invalid. Expected format is a 64 bits integer."),
    ERRNO_83(83, "Provided port number cannot be found."),
    ERRNO_84(84, "Port is already configured this way."),
    ERRNO_85(85, "Invalid tableid value."),
    ERRNO_86(86, "Provided tableid cannot be found."),
    ERRNO_87(87, "Provided table has no features."),
    ERRNO_88(88, "Invalid groupid value."),
    ERRNO_89(89, "Provided groupid cannot be found."),
    ERRNO_90(90, "Provided controller cannot be found."),
    ERRNO_91(91, "Provided controller is already set up."),
    ERRNO_93(93, "ofChannel is not running. Please reboot the switch."),
    ERRNO_94(94, "Invalid queueid value."),
    ERRNO_95(95, "Valid range of min_rate/max_rate is 1 to 65535."),
    ERRNO_96(96, "Valid range of queue weight is 1 to 127."),
    ERRNO_97(97, "Queue not found."),
    ERRNO_98(98, "Invalid meterid value."),
    ERRNO_99(99, "Supported meter flags are 'kbps' and/or 'pktps' and burst and stats."),
    ERRNO_100(100, "Meter flags must include either pktps or kbps."),
    ERRNO_101(101, "Invalid packet multiplier value. Valid range is 1 to 8192."),
    ERRNO_102(102, "Valid tablesizes,tablewidths values are positive integers. "
            + "Valid tabletypes: exactmatch,wildcardmatch."),
    ERRNO_103(103, "The maximum number of tables supported is 120."),
    ERRNO_104(104, "Total size of all tables is too big, please see documentation for more details."),
    ERRNO_108(108, "NoviWare Package is either corrupt or not signed, please download again."),
    ERRNO_109(109, "Switch is booting up, please wait."),
    ERRNO_110(110, "Hostname provided is too long. Maximum is 32 characters."),
    ERRNO_111(111, "Hostname should include only letters, numbers, underscore and dash character."),
    ERRNO_112(112, "Provided meterid cannot be found."),
    ERRNO_113(113, "Invalid Port configuration."),
    ERRNO_114(114, "Provided meter configuration cannot be added."),
    ERRNO_115(115, "Number of meter bands (1-8),."),
    ERRNO_116(116, "Valid meter band types are drop and dscpremark."),
    ERRNO_117(117, "Valid meter band rates: 64 <= rate_value <= 88,704,000 kbps / 1 <= rate_value "
            + "<= 8,300,000 pktps / 1 <= rate_value <= 88,704 mbps."),
    ERRNO_118(118, "Valid meter band burst range is 1 - (rate * 1.005),. "
            + "Maximum allowed value : 8388 kb or 1048576 pkt."),
    ERRNO_119(119, "NoviWare RPM not found, please check the file/path."),
    ERRNO_120(120, "NoviWare upgrade failed, rolling back to default."),
    ERRNO_121(121, "Could not uninstall NoviWare rpm."),
    ERRNO_122(122, "Invalid NoviWare RPM file."),
    ERRNO_123(123, "Valid values for port pause are on/off/rx/tx."),
    ERRNO_124(124, "Port feature is not supppored"),
    ERRNO_125(125, "Provided Meter entry already exists."),
    ERRNO_126(126, "User not found."),
    ERRNO_127(127, "Command Syntax error"),
    ERRNO_128(128, "Can't delete user already logged in."),
    ERRNO_129(129, "Can't update user-group file."),
    ERRNO_130(130, "Failed to delete home directory."),
    ERRNO_131(131, "Could not get packet stats."),
    ERRNO_132(132, "Valid security types are 'none' and 'tls'."),
    ERRNO_134(134, "Process is not running."),
    ERRNO_135(135, "Specified vlan id is reserved. See 'help' for more information."),
    ERRNO_136(136, "Specified VLAN already exists."),
    ERRNO_137(137, "Could not add VLAN. Maximum number of VLANs reached."),
    ERRNO_138(138, "Invalid gateway value."),
    ERRNO_139(139, "Duplicate match fields found in provided values."),
    ERRNO_140(140, "Valid tablewidths values are 10, 20, 40 and 80 for wildcard-match tables and 48 "
            + "for exact-match tables."),
    ERRNO_141(141, "Expecting tablewidths parameter or a valid table size value."),
    ERRNO_142(142, "Table is currently busy."),
    ERRNO_143(143, "Multiple files found in upload directory, only latest NoviWare RPM should be kept."),
    ERRNO_144(144, "Process is currently not running."),
    ERRNO_145(145, "Invalid hostname,please provide valid hostname."),
    ERRNO_146(146, "Invalid upload path provided, path must include NoviWare package file."),
    ERRNO_147(147, "TFTP: File not found on server."),
    ERRNO_148(148, "TFTP server can't be reached."),
    ERRNO_149(149, "Tablesizes value is invalid, please see documentation for more details."),
    ERRNO_150(150, "Tablesizes value missing."),
    ERRNO_151(151, "Number of tablewidth provided should align with tablesizes provided."),
    ERRNO_152(152, "NTP server not configured."),
    ERRNO_153(153, "Host/IP provided not reachable, please check DNS servers or netroute of the switch."),
    ERRNO_154(154, "NTP configuration template missing."),
    ERRNO_155(155, "Could not synchronize time with NTP server, please check the NTP server names and/or "
            + "connection to DNS servers."),
    ERRNO_156(156, "Priority Value must be integer."),
    ERRNO_157(157, "Controller id and group length cannot exceed 16 chars."),
    ERRNO_158(158, "Found duplicate controller id."),
    ERRNO_159(159, "Found duplicate controller priority."),
    ERRNO_160(160, "Controller priority must be an integer value between 1 and 8."),
    ERRNO_161(161, "Found duplicate group name."),
    ERRNO_162(162, "Invalid packet type. Only LLDP is supported."),
    ERRNO_163(163, "Invalid TCP Send buffer value, provided value must be in 2's power and between 2-8192 (KB), "
            + "inclusive."),
    ERRNO_164(164, "Invalid TCP Receive buffer value, provided value must be in 2's power and between 2-8192 (KB), "
            + "inclusive."),
    ERRNO_165(165, "Invalid TCP Send & Receive buffer value, provided value must be in 2's power and "
            + "between 2-8192 (KB), inclusive."),
    ERRNO_166(166, "Could not set tcp buffers value. No connected controllers found to set tcp buffers."),
    ERRNO_167(167, "Max entries reached in TCAM, provide values using [(table_0*width_0/2 + table_1*witdh_1 + .. "
            + "+table_n*width_n),/10< 507904] where table_x and width_x are size and width of the respective tables."),
    ERRNO_168(168, "Only positive integer values are accepted."),
    ERRNO_169(169, "Value must be positive and in hexadecimal format (64 bits),."),
    ERRNO_170(170, "Max limit for controllergroups has reached. Only 16 controllergroups can be configured."),
    ERRNO_171(171, "Controllergroup/controllerid must only include letters, numbers, underscore, dot and dash "
            + "characters and can't be 'all'."),
    ERRNO_172(172, "LLDP is only supported on queue 1 & 6."),
    ERRNO_173(173, "Requested configuration is not supported for this port."),
    ERRNO_174(174, "Bad combination of netipaddr, netmask and gateway."),
    ERRNO_175(175, "Vlan block is not active."),
    ERRNO_176(176, "Vlan not found."),
    ERRNO_177(177, "Invalid Vlan. Supported vlans : 2-4095. Native vid 1 is accepted."),
    ERRNO_178(178, "Port is member multiple Vlans, it can only be TRUNK."),
    ERRNO_179(179, "Invalid UDP payload configuration."),
    ERRNO_180(180, "Packet capture already in progress."),
    ERRNO_181(181, "Packet capture unknown error. Please try again."),
    ERRNO_182(182, "Packet capture not running."),
    ERRNO_183(183, "No controller is configured."),
    ERRNO_184(184, "Unsupported logical port type."),
    ERRNO_185(185, "Valid logicalportno range is 100 to 63487."),
    ERRNO_186(186, "Number of logical ports exceeded."),
    ERRNO_187(187, "Logical port already exists. Please choose different logicalportno."),
    ERRNO_188(188, "The number of physical ports associated with a LAG port cannot exceed 15."),
    ERRNO_189(189, "Invalid LFA. Supported lfa are: macsrc,macdst,macsrcdst,ipsrc,ipdst,ipsrcdst."),
    ERRNO_190(190, "Ports of the LAG can not be duplicated."),
    ERRNO_191(191, "Provided logical port does not exist."),
    ERRNO_192(192, "Invalid user name."),
    ERRNO_194(194, "Command interrupted."),
    ERRNO_195(195, "Could not add route, please check netipaddr,netmask and gateway."),
    ERRNO_196(196, "Could not delete route, please check netipaddr,netmask and gateway."),
    ERRNO_197(197, "file is missing."),
    ERRNO_198(198, "Requested native vid is not created. Only vid 1 and created vlans can be used as native vid."),
    ERRNO_199(199, "No logical ports configured."),
    ERRNO_201(201, "New rpm version must be higher than the current one."),
    ERRNO_202(202, "Hardware version of rpm is incompatible."),
    ERRNO_203(203, "No previous build available. Rollback can be executed only once following an upgrade."),
    ERRNO_204(204, "Invalid configuration file."),
    ERRNO_205(205, "Configuration file version does not match with current software version."),
    ERRNO_206(206, "No AAA server configured."),
    ERRNO_207(207, "Duplicate AAA server configured."),
    ERRNO_208(208, "Maximum three radius servers are supported."),
    ERRNO_209(209, "Password too long should be less than 32 chars."),
    ERRNO_210(210, "Could not add the requested acl rule."),
    ERRNO_211(211, "Bad rule, does not match any existing acl rule."),
    ERRNO_212(212, "Up to 32 acl rules are supported."),
    ERRNO_213(213, "Please provide a valide non zero netmask."),
    ERRNO_214(214, "For IP range, netmask is mandatory."),
    ERRNO_215(215, "Experimenter matchfields format is invalid. Please see 'help' command for supported "
            + "experimenter matchfields."),
    ERRNO_216(216, "Filename provided is invalid. It must contain only letters, numbers, underscore, "
            + "dash or dot characters."),
    ERRNO_217(217, "Could not find import file."),
    ERRNO_218(218, "AAA server not found."),
    ERRNO_219(219, "Duplicate acl rule found."),
    ERRNO_220(220, "Does not match any existing acl rule."),
    ERRNO_221(221, "Invalid IP payload configuration."),
    ERRNO_222(222, "usb interface is not present."),
    ERRNO_223(223, "Requested interface is not active."),
    ERRNO_224(224, "Invalid min_rate value. 'min_rate' cannot be greater than 'max_rate'."),
    ERRNO_225(225, "Feature can only be set for table 0."),
    ERRNO_226(226, "Invalid payload size. Please see 'help' command for supported size."),
    ERRNO_227(227, "The file name is reserved."),
    ERRNO_228(228, "Invalid remotelogserver port."),
    ERRNO_229(229, "Maximum length of username is 16 characters."),
    ERRNO_230(230, "Maximum length of password is 16 characters."),
    ERRNO_231(231, "Valid interval values are 1-23 Hours."),
    ERRNO_232(232, "Maximum length of path is 32 characters."),
    ERRNO_233(233, "Maximum three tacacs+ servers are supported."),
    ERRNO_234(234, "Duplicate auxiliary controller found."),
    ERRNO_235(235, "Priority must be a positive integer value."),
    ERRNO_236(236, "Invalid heading in import file."),
    ERRNO_237(237, "Duplicate heading in import file."),
    ERRNO_238(238, "Invalid section number in import file."),
    ERRNO_239(239, "Invalid record number in import file."),
    ERRNO_240(240, "Invalid value of parameter in import file."),
    ERRNO_241(241, "Invalid parameter in import file."),
    ERRNO_242(242, "Duplicate value of parameter in import file."),
    ERRNO_243(243, "Valid auxiliaryid values range between 1 and 255."),
    ERRNO_244(244, "ofserver is already running."),
    ERRNO_245(245, "ofserver is not running."),
    ERRNO_246(246, "Maximum number of ofclients is 32."),
    ERRNO_247(247, "Duplicate ofclient found."),
    ERRNO_248(248, "ofclient does not exists."),
    ERRNO_249(249, "Could not upload config to remotecfgserver, please check remotecfgserver settings."),
    ERRNO_250(250, "Maximum table search key size reached."),
    ERRNO_251(251, "OFPXMT_OFB_TUNNEL_ID can be added only in table 0."),
    ERRNO_252(252, "The num of tables does not match between PIPELINE and FLOWTABLE."),
    ERRNO_253(253, "Invalid weight. Expected value from 1 to 255."),
    ERRNO_254(254, "System error."),
    ERRNO_255(255, "System error."),
    ERRNO_256(256, "The num of ports does not match with the num of related weights configured in LAG."),
    ERRNO_257(257, "Valid values for version are 'of13', 'of14' , 'of15' , and 'any'."),
    ERRNO_258(258, "Unsupported ofconfig version. Only of111 and of12 are supported."),
    ERRNO_259(259, "No pipeline configuration to be applied."),
    ERRNO_260(260, "Table 0 must be created."),
    ERRNO_261(261, "The maximum number of exact match tables supported is 60."),
    ERRNO_262(262, "The maximum number of wildcard match tables supported is 60."),
    ERRNO_263(263, "Invalid packet type for local port. Only BFD is supported."),
    ERRNO_264(264, "Invalid IDA. Valid values are ipsrcdst, macsrcdst, and inport."),
    ERRNO_265(265, "Unsupported IDA with the current number of flow entries loaded. Valid values are ipsrcdst, "
            + "macsrcdst."),
    ERRNO_266(266, "Invalid discriminator in import file."),
    ERRNO_267(267, "Invalid multiplier in import file."),
    ERRNO_268(268, "Invalid interval in import file."),
    ERRNO_269(269, "Invalid mac address in import file."),
    ERRNO_270(270, "Port already exists in vlan."),
    ERRNO_271(271, "Port does not exist in vlan."),
    ERRNO_272(272, "Table eviction flag is invalid. Only OFPTMPEF_IMPORTANCE is supported."),
    ERRNO_273(273, "Invalid table vacancy down . Expected value is 0 to 99."),
    ERRNO_274(274, "Invalid table vacancy up . Expected value is 0 to 100."),
    ERRNO_275(275, "Invalid table vacancy value . 'vacancy_down' cannot be greater than 'vacancy_up'."),
    ERRNO_276(276, "This command is not licensed."),
    ERRNO_277(277, "Invalid license key."),
    ERRNO_278(278, "License key is not provided."),
    ERRNO_279(279, "Valid snmp types are 'trap' and 'get'."),
    ERRNO_280(280, "Maximum one snmp type 'trap' is supported."),
    ERRNO_281(281, "Maximum eight snmp type 'get' are supported."),
    ERRNO_282(282, "Specified snmp already exists."),
    ERRNO_283(283, "Specified snmp does not exist."),
    ERRNO_284(284, "Provided flowid cannot be found."),
    ERRNO_285(285, "Unsupported field type in the match.Please see 'help' command for supported matchfields."),
    ERRNO_286(286, "Unsupported value in a match field."),
    ERRNO_287(287, "A prerequisite was not met."),
    ERRNO_288(288, "Length problem in match."),
    ERRNO_289(289, "Unsupported match type specified by the match."),
    ERRNO_290(290, "Unsupported mask specified in the match."),
    ERRNO_291(291, "A field type was duplicated."),
    ERRNO_292(292, "Number of valuesmasks provided should align with matchfields provided"),
    ERRNO_293(293, "Matchfield prerequisites are missing."),
    ERRNO_294(294, "Invalid payload size specified by the match."),
    ERRNO_295(295, "Unsupported combination of fields masked or omitted in the match."),
    ERRNO_296(296, "Unsupported output port specified in the flowmod."),
    ERRNO_297(297, "Unsupported output group specified in the flowmod."),
    ERRNO_298(298, "Unsupported flag specified in the flowmod."),
    ERRNO_299(299, "Unsupported instruction type in the flowmod."),
    ERRNO_300(300, "Duplicate instruction type in the flowmod."),
    ERRNO_301(301, "Unsupported value or mask for write medatada instruction."),
    ERRNO_302(302, "Invalid meter id in meter instruction."),
    ERRNO_303(303, "Unsupported action type in the flowmod."),
    ERRNO_304(304, "Too many actions in the flowmod."),
    ERRNO_305(305, "Unsupported port no in output action."),
    ERRNO_306(306, "Invalid max len in output action."),
    ERRNO_307(307, "Unsupported ethertype in push or pop action."),
    ERRNO_308(308, "Invalid mpls ttl value in set mpls ttl action."),
    ERRNO_309(309, "Invalid queueid value in set queue action."),
    ERRNO_310(310, "Invalid groupid value in group action."),
    ERRNO_311(311, "Invalid nw ttl value in set nw ttl action."),
    ERRNO_312(312, "Invalid matchfield type in set field action."),
    ERRNO_313(313, "Invalid matchfield value in set field action."),
    ERRNO_314(314, "Matchfield mask is supported in set field action."),
    ERRNO_315(315, "Invalid Table-ID specified in goto-table instruction."),
    ERRNO_316(316, "Action order is unsupported for the action list in an Apply-Actions instruction."),
    ERRNO_317(317, "Metadata mask value unsupported by datapath."),
    ERRNO_318(318, "Invalid payload offset specified in the action."),
    ERRNO_319(319, "Invalid payload size specified in the action."),
    ERRNO_320(320, "Invalid payload specified in the action."),
    ERRNO_321(321, "Unsupported flag in push tunnel action."),
    ERRNO_322(322, "Invalid mac address in push tunnel action."),
    ERRNO_323(323, "Invalid ip address in push tunnel action."),
    ERRNO_324(324, "Invalid udp port in push tunnel action."),
    ERRNO_325(325, "Invalid vni specified in push tunnel action."),
    ERRNO_326(326, "Invalid label specified in push tunnel action."),
    ERRNO_327(327, "Invalid key specified in push tunnel action."),
    ERRNO_328(328, "Invalid L4 src specified in push tunnel action."),
    ERRNO_329(329, "Invalid tunnel id specified in push tunnel action."),
    ERRNO_330(330, "Invalid msg type specified in push tunnel action."),
    ERRNO_331(331, "Provided table is full."),
    ERRNO_332(332, "Attempt to add overlapping flow with CHECK_OVERLAP flag set."),
    ERRNO_333(333, "Specified buffer does not exist."),
    ERRNO_334(334, "Specified buffer has already been used."),
    ERRNO_335(335, "Invalid packet in the specified buffer."),
    ERRNO_336(336, "Specified group entry does not exist."),
    ERRNO_337(337, "Specified meter entry does not exist."),
    ERRNO_338(338, "Invalid tar."),
    ERRNO_339(339, "Unable to load certificates."),
    ERRNO_340(340, "Unable to load certificate."),
    ERRNO_341(341, "Unable to load CA certificate."),
    ERRNO_342(342, "Unable to load private key."),
    ERRNO_343(343, "Bad security profile."),
    ERRNO_344(344, "Certificate file not found."),
    ERRNO_345(345, "Security profile not found."),
    ERRNO_346(346, "Security profile already exists."),
    ERRNO_347(347, "Security profile is in use by the controller. It must first be deleted."),
    ERRNO_348(348, "TLS security profile cannot be deleted."),
    ERRNO_349(349, "Max MGT users reached, please delete unused MGT user and then re-create."),
    ERRNO_350(350, "Invalid number of bits to copy."),
    ERRNO_351(351, "Invalid offset value."),
    ERRNO_352(352, "Invalid field to copy."),
    ERRNO_353(353, "Bad action settings"),
    ERRNO_354(354, "Invalid copy action."),
    ERRNO_355(355, "Invalid MPLS payload configuration."),
    ERRNO_356(356, "IP, Ethernet and MPLS payload are not supported together in any table."),
    ERRNO_357(357, "Bad action argument."),
    ERRNO_358(358, "Cannot create user with specified username, this username is reserved."),
    ERRNO_359(359, "Hash-fields action contains an invalid combination of fields."),
    ERRNO_360(360, "Error 360: The payload and payload mask for payload matchfields needs to be entered in "
            + "hexadecimal format."),
    ERRNO_361(361, "The name exceeds the maximum amount of characters allowded."),
    ERRNO_362(362, "Priority of flow needs to be a positive integer between 0 and 65535."),
    ERRNO_363(363, "Buffer Id of flow needs to be a positive hexadecimal value between 0x0 and 0xFFFFFFFF."),
    ERRNO_364(364, "Importance of flow needs to be a positive integer between 0 and 65535."),
    ERRNO_365(365, "Idle timeout of flow needs to be a positive integer between 0 and 65535."),
    ERRNO_366(366, "Hard timeout of flow needs to be a positive integer between 0 and 65535."),
    ERRNO_367(367, "Name provided is invalid. It must contain only letters, numbers, underscore, "
            + "dash or dot characters."),
    ERRNO_368(368, "Invalid Ethernet payload configuration."),
    ERRNO_369(369, "Group not deleted because another group is forwarding to it."),
    ERRNO_370(370, "Logical port not deleted because it's being used in a chained group."),
    ERRNO_371(371, "Logical port not updated nor deleted because it's in use"),
    ERRNO_372(372, "Subport number needs to be a positive integer between 1 and 65534."),
    ERRNO_373(373, "Commit rate and max rate need to be a value between 8 and 524288000 kbps (500gbps),."),
    ERRNO_374(374, "The queue max rate needs to be a value between 2 and 10485760 kbps (10 gbps),."),
    ERRNO_375(375, "Service profile provided is invalid."),
    ERRNO_376(376, "Color specific priorities must be between 0 and 3 or notransmit."),
    ERRNO_377(377, "The specified class profile ID is invalid or does not exist."),
    ERRNO_378(378, "The specified class number is invalid."),
    ERRNO_379(379, "Cannot delete subport because it's being used in a class."),
    ERRNO_380(380, "Cannot delete service profile because it's being used in a class profile."),
    ERRNO_381(381, "Cannot delete class profile because it's being used in a class."),
    ERRNO_382(382, "Cannot delete class because it's being used in a flow entry action."),
    ERRNO_383(383, "Cannot delete class because it's being used in a group action."),
    ERRNO_384(384, "Invalid classno value in set class action."),
    ERRNO_385(385, "Could not configure port queue max rate, too many different queue max rates "
            + "(port and class queues),."),
    ERRNO_386(386, "Could not configure class queue max rate, too many different queue maxrates "
            + "(port and class queues),."),
    ERRNO_387(387, "Maximum number of classes exceeded."),
    ERRNO_388(388, "Maximum number of subports exceeded."),
    ERRNO_389(389, "Please specify all three service profile priority weights."),
    ERRNO_390(390, "Could not update logical port type."),
    ERRNO_391(391, "Remote network unreachable via scp."),
    ERRNO_392(392, "Invalid username or password."),
    ERRNO_393(393, "Remote path inaccessible or does not allow write operation."),
    ERRNO_394(394, "Could not find specified file."),
    ERRNO_395(395, "Download failed due to SCP server timeout."),
    ERRNO_396(396, "Action is not allowed"),
    ERRNO_397(397, "Cannot delete logical port because it's being used in a subport."),
    ERRNO_398(398, "Invalid number of bits to swap."),
    ERRNO_399(399, "Invalid offset value."),
    ERRNO_400(400, "Invalid field to swap."),
    ERRNO_401(401, "Bad action settings"),
    ERRNO_402(402, "Invalid swap action."),
    ERRNO_403(403, "Port rxhwtimestamp is not configured."),
    ERRNO_404(404, "Specified snmp user already exists."),
    ERRNO_405(405, "Specified snmp user does not exist."),
    ERRNO_406(406, "Maximum nine snmp users are supported."),
    ERRNO_407(407, "Invalid name."),
    ERRNO_408(408, "Maximum eight snmp groups are supported."),
    ERRNO_409(409, "Specified snmp group name is too long (max 32 chars),."),
    ERRNO_410(410, "Specified snmp group name is reserved."),
    ERRNO_411(411, "Specified snmp group already exists."),
    ERRNO_412(412, "Specified snmp group does not exist."),
    ERRNO_413(413, "Snmp user can only be assigned to an snmp version 3 group."),
    ERRNO_414(414, "Incorrect password length (8-32 chars),."),
    ERRNO_415(415, "Specified snmp community already exists."),
    ERRNO_416(416, "Specified snmp community does not exist."),
    ERRNO_417(417, "Maximum eight snmp communities are supported."),
    ERRNO_418(418, "Specified engine id is invalid."),
    ERRNO_419(419, "Specified group has users assigned to it."),
    ERRNO_420(420, "Unable to delete user. Delete host first to which user is assigned."),
    ERRNO_421(421, "Invalid protocol specified. Valid protocols are ssh, netconf and grpc."),
    ERRNO_422(422, "Invalid target specified. Valid targets are accept, reject and drop."),
    ERRNO_423(423, "The ACL rule cannot be added at the specified rank."),
    ERRNO_424(424, "Maximum length of username is 32 characters."),
    ERRNO_425(425, "Priority weights needs to be integers between 1 and 1023."),
    ERRNO_426(426, "Cannot stop PTP service."),
    ERRNO_427(427, "Cannot start PTP service."),
    ERRNO_428(428, "Invalid livenessthreshold value. Expected value are between 1 and the number of ports specified."),
    ERRNO_429(429, "Maximum length of description is 32 characters."),
    ERRNO_430(430, "Invalid description."),
    ERRNO_431(431, "Invalid ethernet type in push tunnel action."),
    ERRNO_432(432, "Invalid PPPoE code in push tunnel action."),
    ERRNO_433(433, "Invalid PPPoE session id in push tunnel action."),
    ERRNO_434(434, "Invalid PPP protocol id in push tunnel action."),
    ERRNO_435(435, "Invalid port HW timestamp size (in byte),. Expected values are: 4 or 5."),
    ERRNO_436(436, "snmp internal operation error."),
    ERRNO_437(437, "Username is reserved."),
    ERRNO_438(438, "Command failed, please try again."),
    ERRNO_439(439, "This experimenter matchfield value is invalid."),
    ERRNO_440(440, "This matchfield value is invalid."),
    ERRNO_441(441, "Invalid temperature range. Valid range is -100 to 100."),
    ERRNO_442(442, "snmp internal operation error."),
    ERRNO_443(443, "snmp internal operation error."),
    ERRNO_444(444, "snmp internal operation error."),
    ERRNO_445(445, "snmp internal operation error."),
    ERRNO_446(446, "snmp internal operation error."),
    ERRNO_447(447, "snmp internal operation error."),
    ERRNO_448(448, "snmp internal operation error."),
    ERRNO_449(449, "snmp internal operation error."),
    ERRNO_450(450, "snmp internal operation error."),
    ERRNO_451(451, "snmp internal operation error."),
    ERRNO_452(452, "snmp internal operation error."),
    ERRNO_453(453, "snmp internal operation error."),
    ERRNO_454(454, "The specified profile name is invalid."),
    ERRNO_455(455, "The specified authorizedkeys file cannot be found."),
    ERRNO_456(456, "Invalid entry marker."),
    ERRNO_457(457, "The entered queue burst size is invalid, see help for details."),
    ERRNO_458(458, "Could not create session or session timed out."),
    ERRNO_459(459, "Session expired."),
    ERRNO_460(460, "Parameter value provided is invalid. Valid values are between 0 and 60 where 0 timeout."),
    ERRNO_461(461, "Invalid L2TP session id in push tunnel action."),
    ERRNO_462(462, "Invalid L2TP tunnel id in push tunnel action."),
    ERRNO_463(463, "PPPoE and L2TP field matching are not supported together in any table."),
    ERRNO_464(464, "The instruction set contains more than one instruction of each type."),
    ERRNO_465(465, "Match fields combinations is not supported by table."),
    ERRNO_466(466, "System is not responding."),
    ERRNO_467(467, "The specified module is invalid."),
    ERRNO_468(468, "Invalid tcp port in push tunnel action."),
    ERRNO_469(469, "Invalid vlan in push tunnel action."),
    ERRNO_470(470, "Profile name is reserved. Please, choose another name."),
    ERRNO_471(471, "Invalid size. Size needs to be a positive integer between 1 and 100000."),
    ERRNO_472(472, "The maximum number of copy-field profiles is reached (10),."),
    ERRNO_473(473, "The maximum number of swap-field profiles is reached (10),."),
    ERRNO_474(474, "The maximum number of configurations is reached (6),. Overwrite an existing configuration."),
    ERRNO_475(475, "Offset value invalid."),
    ERRNO_476(476, "The maximum number of hash-fields profiles is reached (4),."),
    ERRNO_479(479, "The command could not be executed since ONIE is not installed."),
    ERRNO_480(480, "No more than 25%% of the available CPU cores can be allocated to the Virtual Machine."),
    ERRNO_481(481, "The ramsize of the Virtual Machine must be 512MiB or more and less than 25%% of the total "
            + "system RAM."),
    ERRNO_482(482, "The Virtual Machine's disk cannot exceed 10 GiB or be less than 1 GiB."),
    ERRNO_483(483, "An invalid VNC port was entered for the Virtual Machine."),
    ERRNO_484(484, "The Virtual Machine is currently running, please shut it down and retry."),
    ERRNO_485(485, "The Virtual Machine is not running, please start it and retry."),
    ERRNO_486(486, "A Virtual Machine is already installed on this system."),
    ERRNO_487(487, "No Virtual Machine is installed on this system."),
    ERRNO_488(488, "Could not find the specified installer image."),
    ERRNO_489(489, "The installer image provided for Virtual Machine creation is invalid."),
    ERRNO_490(490, "The specified disk image provided for Virtual Machine import is invalid."),
    ERRNO_491(491, "The operation could not complete due to insufficient free hard drive space."),
    ERRNO_492(492, "There was an error in the Virtual Machine installation process."),
    ERRNO_493(493, "The netmask for the isolated interface is mandatory when setting the network address."),
    ERRNO_494(494, "Could not find a license supporting the NoviWare upgrade."),
    ERRNO_495(495, "Network interface configuration failed: duplicate address in network."),
    ERRNO_496(496, "Invalid flowremoved configuration."),
    ERRNO_497(497, "Invalid cookie filter value provided.");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Gets instance by error code.
     *
     * @param code a response error code.
     * @return instance of {@link ErrorCode}.
     */
    public static ErrorCode getByCode(int code) {
        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid value for ErrorCode"));
    }

}
