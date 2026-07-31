# R8 en mode complet (défaut depuis AGP 8). Aucune règle spécifique n'est nécessaire :
# le projet n'utilise ni réflexion, ni sérialisation, ni JNI.
#
# Conserver les numéros de ligne pour rendre les stack traces de production lisibles.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
