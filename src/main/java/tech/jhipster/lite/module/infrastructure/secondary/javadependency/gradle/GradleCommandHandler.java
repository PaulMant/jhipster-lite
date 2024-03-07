package tech.jhipster.lite.module.infrastructure.secondary.javadependency.gradle;

import static tech.jhipster.lite.module.domain.JHipsterModule.LINE_BREAK;
import static tech.jhipster.lite.module.domain.replacement.ReplacementCondition.always;
import static tech.jhipster.lite.module.infrastructure.secondary.javadependency.gradle.VersionsCatalog.libraryAlias;
import static tech.jhipster.lite.module.infrastructure.secondary.javadependency.gradle.VersionsCatalog.pluginAlias;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.jhipster.lite.module.domain.Indentation;
import tech.jhipster.lite.module.domain.JHipsterProjectFilePath;
import tech.jhipster.lite.module.domain.gradleplugin.GradleCommunityPlugin;
import tech.jhipster.lite.module.domain.gradleplugin.GradleCorePlugin;
import tech.jhipster.lite.module.domain.gradleplugin.GradlePluginConfiguration;
import tech.jhipster.lite.module.domain.javabuild.DependencySlug;
import tech.jhipster.lite.module.domain.javabuild.command.AddDirectJavaDependency;
import tech.jhipster.lite.module.domain.javabuild.command.AddDirectMavenPlugin;
import tech.jhipster.lite.module.domain.javabuild.command.AddGradlePlugin;
import tech.jhipster.lite.module.domain.javabuild.command.AddJavaBuildProfile;
import tech.jhipster.lite.module.domain.javabuild.command.AddJavaDependencyManagement;
import tech.jhipster.lite.module.domain.javabuild.command.AddMavenBuildExtension;
import tech.jhipster.lite.module.domain.javabuild.command.AddMavenPluginManagement;
import tech.jhipster.lite.module.domain.javabuild.command.RemoveDirectJavaDependency;
import tech.jhipster.lite.module.domain.javabuild.command.RemoveJavaDependencyManagement;
import tech.jhipster.lite.module.domain.javabuild.command.SetBuildProperty;
import tech.jhipster.lite.module.domain.javabuild.command.SetVersion;
import tech.jhipster.lite.module.domain.javadependency.JavaDependency;
import tech.jhipster.lite.module.domain.javadependency.JavaDependencyScope;
import tech.jhipster.lite.module.domain.properties.JHipsterProjectFolder;
import tech.jhipster.lite.module.domain.replacement.ContentReplacers;
import tech.jhipster.lite.module.domain.replacement.MandatoryFileReplacer;
import tech.jhipster.lite.module.domain.replacement.MandatoryReplacer;
import tech.jhipster.lite.module.domain.replacement.RegexNeedleBeforeReplacer;
import tech.jhipster.lite.module.domain.replacement.RegexReplacer;
import tech.jhipster.lite.module.infrastructure.secondary.FileSystemReplacer;
import tech.jhipster.lite.module.infrastructure.secondary.javadependency.JavaDependenciesCommandHandler;
import tech.jhipster.lite.shared.error.domain.Assert;

public class GradleCommandHandler implements JavaDependenciesCommandHandler {

  private static final Logger log = LoggerFactory.getLogger(GradleCommandHandler.class);
  private static final String COMMAND = "command";
  private static final String NOT_YET_IMPLEMENTED = "Not yet implemented";
  private static final String BUILD_GRADLE_FILE = "build.gradle.kts";

  private static final Pattern GRADLE_PLUGIN_NEEDLE = Pattern.compile("^\\s+// jhipster-needle-gradle-plugins$", Pattern.MULTILINE);
  private static final Pattern GRADLE_PLUGIN_PROJECT_EXTENSION_CONFIGURATION_NEEDLE = Pattern.compile(
    "^// jhipster-needle-gradle-plugins-configurations$",
    Pattern.MULTILINE
  );
  private static final Pattern GRADLE_IMPLEMENTATION_DEPENDENCY_NEEDLE = Pattern.compile(
    "^\\s+// jhipster-needle-gradle-implementation-dependencies$",
    Pattern.MULTILINE
  );
  private static final Pattern GRADLE_COMPILE_DEPENDENCY_NEEDLE = Pattern.compile(
    "^\\s+// jhipster-needle-gradle-compile-dependencies$",
    Pattern.MULTILINE
  );
  private static final Pattern GRADLE_RUNTIME_DEPENDENCY_NEEDLE = Pattern.compile(
    "^\\s+// jhipster-needle-gradle-runtime-dependencies$",
    Pattern.MULTILINE
  );
  private static final Pattern GRADLE_TEST_DEPENDENCY_NEEDLE = Pattern.compile(
    "^\\s+// jhipster-needle-gradle-test-dependencies$",
    Pattern.MULTILINE
  );
  private static final Pattern GRADLE_PROFILE_ACTIVATION_NEEDLE = Pattern.compile(
    "^// jhipster-needle-profile-activation$",
    Pattern.MULTILINE
  );
  private static final Pattern GRADLE_PROCESS_RESOURCES_NEEDLE = Pattern.compile(
    "^\\s+// jhipster-needle-gradle-process-resources$",
    Pattern.MULTILINE
  );

  private final Indentation indentation;
  private final JHipsterProjectFolder projectFolder;
  private final VersionsCatalog versionsCatalog;
  private final FileSystemReplacer fileReplacer = new FileSystemReplacer();

  public GradleCommandHandler(Indentation indentation, JHipsterProjectFolder projectFolder) {
    Assert.notNull("indentation", indentation);
    Assert.notNull("projectFolder", projectFolder);

    this.indentation = indentation;
    this.projectFolder = projectFolder;
    this.versionsCatalog = new VersionsCatalog(projectFolder);
  }

  @Override
  public void handle(SetVersion command) {
    Assert.notNull(COMMAND, command);

    versionsCatalog.setVersion(command.version());
  }

  @Override
  public void handle(AddDirectJavaDependency command) {
    Assert.notNull(COMMAND, command);

    versionsCatalog.addLibrary(command.dependency());
    addDependencyToBuildGradle(command.dependency());
  }

  private void addDependencyToBuildGradle(JavaDependency dependency) {
    GradleDependencyScope gradleScope = gradleDependencyScope(dependency);

    String dependencyDeclaration = dependencyDeclaration(dependency);
    MandatoryReplacer replacer = new MandatoryReplacer(
      new RegexNeedleBeforeReplacer(
        (contentBeforeReplacement, newText) -> !contentBeforeReplacement.contains(newText),
        needleForGradleDependencyScope(gradleScope)
      ),
      dependencyDeclaration
    );
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  private Pattern needleForGradleDependencyScope(GradleDependencyScope gradleScope) {
    return switch (gradleScope) {
      case GradleDependencyScope.IMPLEMENTATION -> GRADLE_IMPLEMENTATION_DEPENDENCY_NEEDLE;
      case GradleDependencyScope.COMPILE_ONLY -> GRADLE_COMPILE_DEPENDENCY_NEEDLE;
      case GradleDependencyScope.RUNTIME_ONLY -> GRADLE_RUNTIME_DEPENDENCY_NEEDLE;
      case GradleDependencyScope.TEST_IMPLEMENTATION -> GRADLE_TEST_DEPENDENCY_NEEDLE;
    };
  }

  private String dependencyDeclaration(JavaDependency dependency) {
    StringBuilder dependencyDeclaration = new StringBuilder()
      .append(indentation.times(1))
      .append(gradleDependencyScope(dependency).command())
      .append("(");
    if (dependency.scope() == JavaDependencyScope.IMPORT) {
      dependencyDeclaration.append("platform(%s)".formatted(versionCatalogReference(dependency)));
    } else {
      dependencyDeclaration.append(versionCatalogReference(dependency));
    }
    dependencyDeclaration.append(")");

    if (!dependency.exclusions().isEmpty()) {
      dependencyDeclaration.append(" {");
      for (var exclusion : dependency.exclusions()) {
        dependencyDeclaration.append(LINE_BREAK);
        dependencyDeclaration
          .append(indentation.times(2))
          .append("exclude(group = \"%s\", module = \"%s\")".formatted(exclusion.groupId(), exclusion.artifactId()));
      }
      dependencyDeclaration.append(LINE_BREAK);
      dependencyDeclaration.append(indentation.times(1)).append("}");
    }

    return dependencyDeclaration.toString();
  }

  private static String versionCatalogReference(JavaDependency dependency) {
    return "libs.%s".formatted(applyVersionCatalogReferenceConvention(libraryAlias(dependency)));
  }

  private static String applyVersionCatalogReferenceConvention(String rawVersionCatalogReference) {
    return rawVersionCatalogReference.replace("-", ".");
  }

  private static GradleDependencyScope gradleDependencyScope(JavaDependency dependency) {
    return switch (dependency.scope()) {
      case TEST -> GradleDependencyScope.TEST_IMPLEMENTATION;
      case PROVIDED -> GradleDependencyScope.COMPILE_ONLY;
      case RUNTIME -> GradleDependencyScope.RUNTIME_ONLY;
      default -> GradleDependencyScope.IMPLEMENTATION;
    };
  }

  @Override
  public void handle(RemoveDirectJavaDependency command) {
    versionsCatalog.retrieveDependencySlugsFrom(command.dependency()).forEach(this::removeDependencyFromBuildGradle);
    versionsCatalog.removeLibrary(command.dependency());
  }

  private void removeDependencyFromBuildGradle(DependencySlug dependencySlug) {
    String scopePattern = Stream.of(GradleDependencyScope.values())
      .map(GradleDependencyScope::command)
      .collect(Collectors.joining("|", "(?:", ")"));
    Pattern dependencyLinePattern = Pattern.compile(
      "^\\s+%s\\((?:platform\\()?libs\\.%s\\)?\\)(?:\\s+\\{(?:\\s+exclude\\([^)]*\\))+\\s+\\})?$".formatted(
          scopePattern,
          dependencySlug.slug().replace("-", "\\.")
        ),
      Pattern.MULTILINE
    );
    MandatoryReplacer replacer = new MandatoryReplacer(new RegexReplacer(always(), dependencyLinePattern), "");
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  @Override
  public void handle(RemoveJavaDependencyManagement command) {
    versionsCatalog.retrieveDependencySlugsFrom(command.dependency()).forEach(this::removeDependencyFromBuildGradle);
    versionsCatalog.removeLibrary(command.dependency());
  }

  @Override
  public void handle(AddJavaDependencyManagement command) {
    versionsCatalog.addLibrary(command.dependency());
    addDependencyToBuildGradle(command.dependency());
  }

  @Override
  public void handle(AddDirectMavenPlugin command) {
    // Maven specific commands are ignored
  }

  @Override
  public void handle(AddMavenPluginManagement command) {
    // Maven commands are ignored
  }

  @Override
  public void handle(SetBuildProperty command) {
    throw new NotImplementedException(NOT_YET_IMPLEMENTED);
  }

  @Override
  public void handle(AddMavenBuildExtension command) {
    // Maven commands are ignored
  }

  @Override
  public void handle(AddJavaBuildProfile command) {
    Assert.notNull(COMMAND, command);

    prerequisiteBuildProfile();
    addProfile(command);
  }

  private void prerequisiteBuildProfile() {
    addProfileIdentifier();
    addProcessResourcesApplicationReplacer();
    enablePrecompiledScriptPlugins();
  }

  @Override
  public void handle(AddGradlePlugin command) {
    Assert.notNull(COMMAND, command);

    switch (command.plugin()) {
      case GradleCorePlugin plugin -> declarePlugin(plugin.id().get());
      case GradleCommunityPlugin plugin -> {
        declarePlugin("alias(libs.plugins.%s)".formatted(applyVersionCatalogReferenceConvention(pluginAlias(plugin))));
        versionsCatalog.addPlugin(plugin);
      }
    }
    command.plugin().configuration().ifPresent(this::addPluginConfiguration);
    command.toolVersion().ifPresent(version -> handle(new SetVersion(version)));
    command.pluginVersion().ifPresent(version -> handle(new SetVersion(version)));
  }

  private void declarePlugin(String pluginDeclaration) {
    MandatoryReplacer replacer = new MandatoryReplacer(
      new RegexNeedleBeforeReplacer(
        (contentBeforeReplacement, newText) -> !contentBeforeReplacement.contains(newText),
        GRADLE_PLUGIN_NEEDLE
      ),
      indentation.times(1) + pluginDeclaration
    );
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  private void addPluginConfiguration(GradlePluginConfiguration pluginConfiguration) {
    MandatoryReplacer replacer = new MandatoryReplacer(
      new RegexNeedleBeforeReplacer(
        (contentBeforeReplacement, newText) -> !contentBeforeReplacement.contains(newText),
        GRADLE_PLUGIN_PROJECT_EXTENSION_CONFIGURATION_NEEDLE
      ),
      LINE_BREAK + pluginConfiguration.get()
    );
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  private void addProfileIdentifier() {
    MandatoryReplacer replacer = new MandatoryReplacer(
      new RegexNeedleBeforeReplacer(
        (contentBeforeReplacement, newText) -> !contentBeforeReplacement.contains(newText),
        GRADLE_PROFILE_ACTIVATION_NEEDLE
      ),
      """
      val profiles = (project.findProperty("profiles") as String? ?: "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }\
      """
    );
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  private void addProcessResourcesApplicationReplacer() {
    MandatoryReplacer replacer = new MandatoryReplacer(
      new RegexNeedleBeforeReplacer(
        (contentBeforeReplacement, newText) -> !contentBeforeReplacement.contains(newText),
        GRADLE_PROCESS_RESOURCES_NEEDLE
      ),
      """
        filesMatching("**/application.yml") {
          filter {
            // jhipster-needle-gradle-profiles-properties
          }
        }\
      """
    );
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  private void enablePrecompiledScriptPlugins() {
    addFileToProject("generator/buildtool/gradle/buildSrc/build.gradle.kts.template", "buildSrc/build.gradle.kts");
  }

  private void addProfile(AddJavaBuildProfile command) {
    addProfileActivation(command);
    addProfileBuildGradleFile(command);
  }

  private void addProfileActivation(AddJavaBuildProfile command) {
    String profileTemplate =
      """
      if (profiles.contains("%s")) {
        apply(plugin = "profile-%s")
      }\
      """;
    String profileTemplateFilled = profileTemplate.formatted(command.buildProfileId(), command.buildProfileId());
    MandatoryReplacer replacer = new MandatoryReplacer(
      new RegexNeedleBeforeReplacer(
        (contentBeforeReplacement, newText) -> !contentBeforeReplacement.contains(newText),
        GRADLE_PROFILE_ACTIVATION_NEEDLE
      ),
      profileTemplateFilled
    );
    fileReplacer.handle(
      projectFolder,
      ContentReplacers.of(new MandatoryFileReplacer(new JHipsterProjectFilePath(BUILD_GRADLE_FILE), replacer))
    );
  }

  private void addProfileBuildGradleFile(AddJavaBuildProfile command) {
    addFileToProject(
      "generator/buildtool/gradle/buildSrc/src/main/kotlin/profile.gradle.kts.template",
      "buildSrc/src/main/kotlin/profile-%s.gradle.kts".formatted(command.buildProfileId())
    );
  }

  private void addFileToProject(String source, String destination) {
    Path sourcePath = Paths.get("src/main/resources").resolve(source);
    Path destinationPath = Paths.get(projectFolder.get()).resolve(destination);
    if (Files.exists(destinationPath)) {
      log.info("Not coping {} since {} already exists", sourcePath, destinationPath);

      return;
    }
    try {
      Files.createDirectories(destinationPath.getParent());
    } catch (IOException e) {
      throw new UnableCreateFolderException(destinationPath.getParent().toString(), e);
    }

    try {
      Files.copy(sourcePath, destinationPath);
    } catch (IOException e) {
      throw new UnableCopyFileException(sourcePath.toString(), destinationPath.toString(), e);
    }
  }
}
