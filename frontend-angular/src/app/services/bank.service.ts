import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { BankAccount, BankTransaction, AuditEvent, ChatMessage, CreateAccountRequest, TransferRequest } from '../models/bank.models';

@Injectable({
  providedIn: 'root'
})
export class BankService {
  private gatewayUrl = 'http://localhost:8080';

  // Demo accounts data
  private accountsState: BankAccount[] = [
    {
      id: '11111111-1111-1111-1111-111111111111',
      customerReference: 'CUST-1001',
      status: 'ACTIVE',
      currency: 'EUR',
      balance: 5000.00
    },
    {
      id: '22222222-2222-2222-2222-222222222222',
      customerReference: 'CUST-1002',
      status: 'ACTIVE',
      currency: 'EUR',
      balance: 1500.00
    },
    {
      id: '99999999-9999-9999-9999-999999999999',
      customerReference: 'CUST-1003',
      status: 'BLOCKED',
      currency: 'EUR',
      balance: 200.00
    }
  ];

  // Demo transactions
  private transactionsState: BankTransaction[] = [
    {
      id: 'tx-001',
      accountId: '11111111-1111-1111-1111-111111111111',
      amount: 250.00,
      currency: 'EUR',
      merchantCategory: 'SUPERMARKET',
      country: 'MA',
      createdAt: new Date(Date.now() - 3600000).toISOString()
    },
    {
      id: 'tx-002',
      accountId: '22222222-2222-2222-2222-222222222222',
      amount: 45.00,
      currency: 'EUR',
      merchantCategory: 'RESTAURANT',
      country: 'FR',
      createdAt: new Date(Date.now() - 7200000).toISOString()
    }
  ];

  // Demo Kafka Audit Events
  private auditEventsState: AuditEvent[] = [
    {
      id: 'audit-001',
      eventId: 'e9a0846d-a1c2-48f8-b57e-399d8b375e21',
      eventType: 'TransactionCreated',
      transactionId: 'tx-001',
      accountId: '11111111-1111-1111-1111-111111111111',
      amount: 250.00,
      currency: 'EUR',
      occurredAt: new Date(Date.now() - 3600000).toISOString(),
      receivedAt: new Date(Date.now() - 3599000).toISOString()
    }
  ];

  private accounts$ = new BehaviorSubject<BankAccount[]>(this.accountsState);
  private transactions$ = new BehaviorSubject<BankTransaction[]>(this.transactionsState);
  private auditEvents$ = new BehaviorSubject<AuditEvent[]>(this.auditEventsState);

  constructor(private http: HttpClient) {}

  getAccounts(): Observable<BankAccount[]> {
    return this.http.get<BankAccount[]>(`${this.gatewayUrl}/api/accounts`).pipe(
      tap(accs => this.accounts$.next(accs)),
      catchError(() => this.accounts$.asObservable())
    );
  }

  createAccount(req: CreateAccountRequest): Observable<BankAccount> {
    const newAcc: BankAccount = {
      id: crypto.randomUUID ? crypto.randomUUID() : 'acc-' + Math.floor(Math.random() * 10000),
      customerReference: req.customerReference,
      status: 'ACTIVE',
      currency: req.currency.toUpperCase(),
      balance: req.initialBalance
    };

    return this.http.post<BankAccount>(`${this.gatewayUrl}/api/accounts`, req).pipe(
      catchError(() => of(newAcc)),
      tap(created => {
        this.accountsState = [created, ...this.accountsState];
        this.accounts$.next(this.accountsState);
      })
    );
  }

  toggleAccountStatus(id: string): Observable<BankAccount> {
    const acc = this.accountsState.find(a => a.id === id);
    if (acc) {
      acc.status = acc.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
      this.accounts$.next([...this.accountsState]);
      return of(acc);
    }
    throw new Error('Compte introuvable');
  }

  getTransactions(): Observable<BankTransaction[]> {
    return this.http.get<BankTransaction[]>(`${this.gatewayUrl}/api/transactions`).pipe(
      tap(txs => this.transactions$.next(txs)),
      catchError(() => this.transactions$.asObservable())
    );
  }

  transfer(req: TransferRequest): Observable<any> {
    if (req.amount <= 0) {
      throw new Error('Le montant doit être strictement positif');
    }
    if (req.amount > 1000) {
      throw new Error('Le montant dépasse le plafond configuré de 1000.0 EUR');
    }

    const src = this.accountsState.find(a => a.id === req.sourceAccountId);
    const tgt = this.accountsState.find(a => a.id === req.targetAccountId);

    if (!src || src.status !== 'ACTIVE') {
      throw new Error('Le compte émetteur est invalide ou bloqué');
    }
    if (!tgt || tgt.status !== 'ACTIVE') {
      throw new Error('Le compte destinataire est invalide ou bloqué');
    }
    if (src.balance < req.amount) {
      throw new Error('Solde insuffisant sur le compte émetteur');
    }

    // Effectuer le virement localement
    src.balance -= req.amount;
    tgt.balance += req.amount;
    this.accounts$.next([...this.accountsState]);

    const txDebit: BankTransaction = {
      id: 'tx-deb-' + Date.now(),
      accountId: src.id,
      amount: req.amount,
      currency: req.currency,
      merchantCategory: 'TRANSFER_DEBIT',
      country: 'MA',
      createdAt: new Date().toISOString()
    };

    const txCredit: BankTransaction = {
      id: 'tx-crd-' + Date.now(),
      accountId: tgt.id,
      amount: req.amount,
      currency: req.currency,
      merchantCategory: 'TRANSFER_CREDIT',
      country: 'MA',
      createdAt: new Date().toISOString()
    };

    this.transactionsState = [txDebit, txCredit, ...this.transactionsState];
    this.transactions$.next(this.transactionsState);

    // Émettre un événement d'audit Kafka
    const auditEv: AuditEvent = {
      id: 'audit-' + Date.now(),
      eventId: crypto.randomUUID ? crypto.randomUUID() : 'evt-' + Date.now(),
      eventType: 'TransactionCreated',
      transactionId: txDebit.id,
      accountId: src.id,
      amount: req.amount,
      currency: req.currency,
      occurredAt: txDebit.createdAt,
      receivedAt: new Date().toISOString()
    };
    this.auditEventsState = [auditEv, ...this.auditEventsState];
    this.auditEvents$.next(this.auditEventsState);

    return of({ status: 'SUCCESS', message: 'Virement de ' + req.amount + ' ' + req.currency + ' exécuté avec succès' });
  }

  getAuditEvents(): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`${this.gatewayUrl}/api/audit/events`).pipe(
      tap(events => this.auditEvents$.next(events)),
      catchError(() => this.auditEvents$.asObservable())
    );
  }

  askAssistant(query: string): Observable<ChatMessage> {
    const qLower = query.toLowerCase();
    let reply = "Bonjour ! Je suis votre Assistant Bancaire Agentique.";
    let toolUsed = 'none';

    if (qLower.includes('solde') || qLower.includes('compte')) {
      toolUsed = 'get_account_by_id';
      const firstAcc = this.accountsState[0];
      reply = `Votre compte principal (${firstAcc.customerReference}) a un solde actuel de ${firstAcc.balance.toFixed(2)} ${firstAcc.currency} avec statut ${firstAcc.status}.`;
    } else if (qLower.includes('virement') || qLower.includes('transfer')) {
      toolUsed = 'transfer_funds';
      reply = `Virement bancaire simulé avec succès via le tool MCP transfer_funds ! Le solde et la file Kafka ont été mis à jour.`;
    } else if (qLower.includes('créer') || qLower.includes('ouvrir')) {
      toolUsed = 'create_bank_account';
      reply = `Compte bancaire créé avec succès via l'outil MCP create_bank_account pour le client spécifié.`;
    }

    const assistantMsg: ChatMessage = {
      id: 'msg-' + Date.now(),
      sender: 'assistant',
      text: reply,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      toolUsed: toolUsed
    };

    return of(assistantMsg);
  }
}
