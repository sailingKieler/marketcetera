package org.rubypeople.rdt.internal.launching;

import org.eclipse.osgi.util.NLS;

public class LaunchingMessages {

	private static final String BUNDLE_NAME = LaunchingMessages.class.getName();

	public static String RdtLaunchingPlugin_processTerminatedBecauseNoDebuggerConnection;
	public static String RdtLaunchingPlugin_internalErrorOccurred;
	public static String RdtLaunchingPlugin_noInterpreterSelected;
	public static String RdtLaunchingPlugin_interpreterNotFound;
	public static String RdtLaunchingPlugin_noInterpreterSelectedTitle;
	public static String RubyRuntime_badFormat;
	public static String RubyRuntime_VM_type_element_with_unknown_id_1;
	public static String RubyRuntime_VM_element_specified_with_no_id_attribute_2;
	public static String RubyRuntime_exceptionOccurred;
	public static String vmInstall_assert_typeNotNull;
	public static String vmInstall_assert_idNotNull;
	public static String AbstractInterpreterInstall_0;
	public static String AbstractInterpreterInstall_1;
	public static String AbstractInterpreterInstall_3;
	public static String AbstractInterpreterInstall_4;
	public static String LaunchingPlugin_33;
	public static String LaunchingPlugin_34;
	public static String RubyRuntime_exceptionsOccurred;
	public static String vmInstallType_duplicateVM;
	public static String StandardVMType_Standard_VM_3;
	public static String StandardVMType_Not_a_JDK_Root__Java_executable_was_not_found_1;
	public static String StandardVMType_ok_2;
	public static String StandardVMType_Not_a_JDK_root__System_library_was_not_found__1;
	public static String AbstractVMRunner_0;
	public static String vmRunnerConfig_assert_classNotNull;
	public static String vmRunnerConfig_assert_classPathNotNull;
	public static String vmRunnerConfig_assert_vmArgsNotNull;
	public static String vmRunnerConfig_assert_programArgsNotNull;
	public static String StandardVMRunner__0__at_localhost__1__1;
	public static String StandardVMRunner__0____1___2;
	public static String StandardVMRunner_Specified_working_directory_does_not_exist_or_is_not_a_directory___0__3;
	public static String StandardVMRunner_Unable_to_locate_executable_for__0__1;
	public static String StandardVMRunner_Specified_executable__0__does_not_exist_for__1__4;
	public static String StandardVMRunner_Launching_VM____1;
	public static String StandardVMRunner_Constructing_command_line____2;
	public static String StandardVMRunner_Starting_virtual_machine____3;
	public static String JavaLocalApplicationLaunchConfigurationDelegate_Creating_source_locator____2;
	public static String JavaLocalApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1;
    public static String AbstractJavaLaunchConfigurationDelegate_The_specified_JRE_installation_does_not_exist_4;
	public static String AbstractJavaLaunchConfigurationDelegate_JRE_home_directory_not_specified_for__0__5;
	public static String AbstractJavaLaunchConfigurationDelegate_JRE_home_directory_for__0__does_not_exist___1__6;
	public static String JavaLocalApplicationLaunchConfigurationDelegate_0;
	public static String JavaRuntime_Specified_VM_install_type_does_not_exist___0__2;
	public static String JavaRuntime_Specified_VM_install_not_found__type__0___name__1__2;
	public static String JavaRuntime_VM_not_fully_specified_in_launch_configuration__0____missing_VM_name__Reverting_to_default_VM__1;
	public static String JavaRuntime_28;
	public static String JavaRuntime_Launch_configuration__0__references_non_existing_project__1___1;
	public static String AbstractJavaLaunchConfigurationDelegate_Working_directory_does_not_exist___0__12;
	public static String AbstractJavaLaunchConfigurationDelegate_Main_type_not_specified_11;
	public static String RuntimeLoadpathEntry_Illegal_classpath_entry__0__1;
	public static String RuntimeLoadpathEntry_Unable_to_recover_runtime_class_path_entry_type_2;
	public static String RuntimeLoadpathEntry_Unable_to_recover_runtime_class_path_entry_location_3;
	public static String RuntimeLoadpathEntry_Unable_to_recover_runtime_class_path_entry___missing_project_name_4;
	public static String RuntimeLoadpathEntry_Unable_to_recover_runtime_class_path_entry___missing_archive_path_5;
	public static String RuntimeLoadpathEntry_Unable_to_recover_runtime_class_path_entry___missing_variable_name_6;
	public static String RuntimeLoadpathEntry_An_exception_occurred_generating_runtime_classpath_memento_8;
	public static String DefaultProjectLoadpathEntry_4;
	public static String DefaultProjectLoadpathEntry_2;
	public static String DefaultProjectLoadpathEntry_3;
	public static String JavaRuntime_26;
	public static String JavaRuntime_31;
	public static String JavaRuntime_32;
	public static String LaunchingPlugin_32;
	public static String JavaRuntime_Classpath_references_non_existant_archive___0__4;
	public static String JavaRuntime_Classpath_references_non_existant_project___0__3;
	public static String JavaRuntime_Could_not_resolve_classpath_container___0__1;
	public static String StandardVMDebugger_Launching_VM____1;
	public static String StandardVMDebugger_Finding_free_socket____2;
	public static String StandardVMDebugger_Could_not_find_a_free_socket_for_the_debugger_1;
	public static String StandardVMDebugger_Constructing_command_line____3;
	public static String StandardVMDebugger_Starting_virtual_machine____4;
	public static String StandardVMDebugger_Establishing_debug_connection____5;
	public static String StandardVMDebugger_Couldn__t_connect_to_VM_4;
	public static String StandardVMDebugger_Couldn__t_connect_to_VM_5;
	public static String LaunchingPlugin_0;
	public static String LaunchingPlugin_1;
	
	private LaunchingMessages() {}

	static {
		NLS.initializeMessages(BUNDLE_NAME, LaunchingMessages.class);
	}
}
