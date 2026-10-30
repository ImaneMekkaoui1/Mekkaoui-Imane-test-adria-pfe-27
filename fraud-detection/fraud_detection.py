"""
=============================================================================
Épreuve Technique de Sélection ADRIA - Partie H : Détection de Fraude avec IA
Auteur : Imane Mekkaoui
=============================================================================
"""

import os
import pandas as pd
import numpy as np

from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
    average_precision_score
)

def run_fraud_detection():
    data_path = os.path.join(os.path.dirname(__file__), "bank_transactions.csv")
    print("=" * 70)
    print("1. CHARGEMENT DES DONNÉES")
    print("=" * 70)
    df = pd.read_csv(data_path)
    print(f"Jeu de données chargé avec succès : {df.shape[0]} lignes, {df.shape[1]} colonnes.")
    print(df.head(3))

    print("\n" + "=" * 70)
    print("2. PROPORTION DE FRAUDES & VALEURS MANQUANTES")
    print("=" * 70)
    fraud_counts = df['is_fraud'].value_counts()
    fraud_pct = df['is_fraud'].mean() * 100
    print(f"Distribution de la cible (is_fraud) :")
    print(f"  - Non-fraude (0) : {fraud_counts.get(0, 0)} ({100 - fraud_pct:.2f}%)")
    print(f"  - Fraude (1)     : {fraud_counts.get(1, 0)} ({fraud_pct:.2f}%)")
    print(f"\nValeurs manquantes par colonne :")
    print(df.isnull().sum()[df.isnull().sum() > 0])

    print("\n" + "=" * 70)
    print("3 & 4. EXCLUSION DES IDENTIFIANTS & SÉPARATION STRATIFIÉE")
    print("=" * 70)
    # Exclure transaction_id et account_id des variables explicatives
    features = [c for c in df.columns if c not in ['transaction_id', 'account_id', 'is_fraud']]
    X = df[features]
    y = df['is_fraud']

    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.25,
        random_state=42,
        stratify=y  # Séparation stratifiée pour préserver le ratio de fraude
    )
    print(f"Taille de l'ensemble d'entraînement : {X_train.shape[0]} transactions")
    print(f"Taille de l'ensemble de test        : {X_test.shape[0]} transactions")
    print(f"Taux de fraude Train : {y_train.mean()*100:.2f}% | Test : {y_test.mean()*100:.2f}%")

    print("\n" + "=" * 70)
    print("5. CONSTRUCTION DU PIPELINE DE PRÉTRAITEMENT")
    print("=" * 70)
    numeric_features = ['amount', 'hour', 'transactions_last_24h', 'average_amount_last_7d']
    categorical_features = ['country', 'merchant_category']

    numeric_transformer = Pipeline(steps=[
        ('imputer', SimpleImputer(strategy='median')),
        ('scaler', StandardScaler())
    ])

    categorical_transformer = Pipeline(steps=[
        ('imputer', SimpleImputer(strategy='constant', fill_value='UNKNOWN')),
        ('encoder', OneHotEncoder(handle_unknown='ignore', sparse_output=False))
    ])

    preprocessor = ColumnTransformer(transformers=[
        ('num', numeric_transformer, numeric_features),
        ('cat', categorical_transformer, categorical_features)
    ])

    print("Pipeline configuré : Imputation médiane + StandardScaler (num), OneHot (cat)")

    print("\n" + "=" * 70)
    print("6. ENTRAÎNEMENT DU MODÈLE (RandomForestClassifier)")
    print("=" * 70)
    model = Pipeline(steps=[
        ('preprocessor', preprocessor),
        ('classifier', RandomForestClassifier(
            n_estimators=100,
            random_state=42,
            class_weight='balanced',  # Prise en compte du déséquilibre des classes
            max_depth=8
        ))
    ])

    # Le prétraitement et le modèle sont ajustés UNIQUEMENT sur les données d'entraînement
    model.fit(X_train, y_train)
    print("Modèle entraîné avec succès sur l'ensemble d'entraînement.")

    print("\n" + "=" * 70)
    print("7. ÉVALUATION DU MODÈLE SUR L'ENSEMBLE DE TEST")
    print("=" * 70)
    y_pred = model.predict(X_test)
    y_proba = model.predict_proba(X_test)[:, 1]

    prec = precision_score(y_test, y_pred, pos_label=1)
    rec = recall_score(y_test, y_pred, pos_label=1)
    f1 = f1_score(y_test, y_pred, pos_label=1)
    ap = average_precision_score(y_test, y_proba)
    cm = confusion_matrix(y_test, y_pred)

    print(f"Précision (Classe Fraude)   : {prec:.4f}")
    print(f"Rappel (Classe Fraude)      : {rec:.4f}")
    print(f"F1-Score (Classe Fraude)    : {f1:.4f}")
    print(f"Average Precision (PR-AUC)  : {ap:.4f}")
    print("\nMatrice de Confusion :")
    print(f"[[Vrais Négatifs: {cm[0,0]}  Faux Positifs: {cm[0,1]}]")
    print(f" [Faux Négatifs:  {cm[1,0]}  Vrais Positifs: {cm[1,1]}]]")

    print("\nRapport de classification complet :")
    print(classification_report(y_test, y_pred, target_names=["Légitime (0)", "Fraude (1)"]))

    print("\n" + "=" * 70)
    print("RÉPONSES AUX QUESTIONS D'ANALYSE (PARTIE H)")
    print("=" * 70)
    print("""
1. Pourquoi l'accuracy seule est-elle insuffisante ?
   Dans un contexte bancaire où la fraude ne représente que 2% des transactions, un modèle trivial 
   qui prédirait systématiquement '0' (aucune fraude) obtiendrait 98% d'accuracy tout en manquant 
   100% des fraudes. L'accuracy masque totalement l'incapacité du modèle à détecter la classe minoritaire.

2. Quel compromis existe entre faux positifs et fraudes non détectées ?
   - Les Faux Négatifs (fraudes non détectées) engendrent des pertes financières directes pour la banque 
     et un préjudice pour le client.
   - Les Faux Positifs (transactions légitimes bloquées par erreur) provoquent des frictions clients, 
     des abandons d'achat et un coût opérationnel accru pour les équipes du support client.
   Le compromis dépend de l'appétence au risque et de la politique de sécurité de l'établissement.

3. Quel est l'effet possible d'une diminution du seuil de décision ?
   Diminuer le seuil (ex: passer de 0.5 à 0.2) rend le modèle plus sensible :
   - Le Rappel (Recall) augmente : on intercepte davantage de fraudes réelles.
   - La Précision diminue : le nombre de faux positifs (alertes injustifiées) augmente en contrepartie.

4. Pourquoi privilégier une validation temporelle sur des données bancaires réelles ?
   Les transactions bancaires sont séquentielles et soumises au concept drift (évolution des comportements 
   des fraudeurs, saisonnalité des achats). Une séparation aléatoire (stratifiée) entraîne une fuite 
   d'information temporelle vers le passé (data leakage). Une validation temporelle (Train sur le passé, 
   Test sur le futur) reflète fidèlement les conditions réelles de mise en production.
    """)

if __name__ == '__main__':
    run_fraud_detection()

