import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BankService } from './services/bank.service';
import { BankAccount, BankTransaction, AuditEvent, ChatMessage, CreateAccountRequest, TransferRequest } from './models/bank.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  activeTab: 'accounts' | 'transactions' | 'audit' | 'chatbot' = 'accounts';

  accounts: BankAccount[] = [];
  transactions: BankTransaction[] = [];
  auditEvents: AuditEvent[] = [];

  // Form states
  newAccount: CreateAccountRequest = {
    customerReference: '',
    currency: 'EUR',
    initialBalance: 1000
  };

  transferData: TransferRequest = {
    sourceAccountId: '',
    targetAccountId: '',
    amount: 100,
    currency: 'EUR'
  };

  transferFeedback: { success: boolean; message: string } | null = null;
  accountFeedback: { success: boolean; message: string } | null = null;

  // Chatbot state
  chatMessages: ChatMessage[] = [
    {
      id: 'init-1',
      sender: 'assistant',
      text: 'Bonjour ! Je suis l\'assistant bancaire IA d\'Adria Bank. Comment puis-je vous aider aujourd\'hui ?',
      timestamp: '10:00'
    }
  ];
  userInput: string = '';

  constructor(private bankService: BankService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.bankService.getAccounts().subscribe(accs => {
      this.accounts = accs;
      if (accs.length >= 2) {
        this.transferData.sourceAccountId = accs[0].id;
        this.transferData.targetAccountId = accs[1].id;
      }
    });

    this.bankService.getTransactions().subscribe(txs => this.transactions = txs);
    this.bankService.getAuditEvents().subscribe(evts => this.auditEvents = evts);
  }

  get totalBalance(): number {
    return this.accounts.reduce((sum, a) => sum + (a.status === 'ACTIVE' ? a.balance : 0), 0);
  }

  get activeAccountsCount(): number {
    return this.accounts.filter(a => a.status === 'ACTIVE').length;
  }

  get blockedAccountsCount(): number {
    return this.accounts.filter(a => a.status === 'BLOCKED').length;
  }

  handleCreateAccount(): void {
    if (!this.newAccount.customerReference) {
      this.accountFeedback = { success: false, message: 'La référence client est obligatoire.' };
      return;
    }

    this.bankService.createAccount(this.newAccount).subscribe({
      next: (acc) => {
        this.accountFeedback = { success: true, message: `Compte ${acc.customerReference} ouvert avec succès !` };
        this.newAccount = { customerReference: '', currency: 'EUR', initialBalance: 1000 };
        this.loadData();
        setTimeout(() => this.accountFeedback = null, 4000);
      },
      error: (err) => {
        this.accountFeedback = { success: false, message: err.message || 'Erreur lors de la création' };
      }
    });
  }

  handleToggleStatus(id: string): void {
    this.bankService.toggleAccountStatus(id).subscribe(() => this.loadData());
  }

  handleTransfer(): void {
    try {
      this.bankService.transfer(this.transferData).subscribe({
        next: (res) => {
          this.transferFeedback = { success: true, message: res.message };
          this.loadData();
          setTimeout(() => this.transferFeedback = null, 4000);
        },
        error: (err) => {
          this.transferFeedback = { success: false, message: err.message };
        }
      });
    } catch (e: any) {
      this.transferFeedback = { success: false, message: e.message };
    }
  }

  handleSendMessage(): void {
    const text = this.userInput.trim();
    if (!text) return;

    const userMsg: ChatMessage = {
      id: 'usr-' + Date.now(),
      sender: 'user',
      text: text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    this.chatMessages.push(userMsg);
    this.userInput = '';

    this.bankService.askAssistant(text).subscribe(assistantMsg => {
      this.chatMessages.push(assistantMsg);
    });
  }

  sendQuickPrompt(promptText: string): void {
    this.userInput = promptText;
    this.handleSendMessage();
  }
}
