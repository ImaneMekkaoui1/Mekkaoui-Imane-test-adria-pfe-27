export interface BankAccount {
  id: string;
  customerReference: string;
  status: 'ACTIVE' | 'BLOCKED';
  currency: string;
  balance: number;
}

export interface BankTransaction {
  id: string;
  accountId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  country: string;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  eventId: string;
  eventType: string;
  transactionId: string;
  accountId: string;
  amount: number;
  currency: string;
  occurredAt: string;
  receivedAt: string;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
  toolUsed?: string;
}

export interface CreateAccountRequest {
  customerReference: string;
  currency: string;
  initialBalance: number;
}

export interface TransferRequest {
  sourceAccountId: string;
  targetAccountId: string;
  amount: number;
  currency: string;
}
