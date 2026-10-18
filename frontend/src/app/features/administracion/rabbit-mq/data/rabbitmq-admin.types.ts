export interface ApiMessage {
  message: string;
  resource: string | null;
}

export interface QueueInfo {
  name: string;
  durable: boolean;
  exclusive: boolean;
  autoDelete: boolean;
  messages: number;
  messagesReady: number;
  messagesUnacknowledged: number;
  consumers: number;
}

export interface ExchangeInfo {
  name: string;
  type: string;
  durable: boolean;
  autoDelete: boolean;
}

export interface BindingInfo {
  source: string;
  destination: string;
  destinationType: string;
  routingKey: string;
}

export interface OverviewInfo {
  queueTotals?: {
    messages: number;
    messagesReady: number;
    messagesUnacknowledged: number;
  };
  objectTotals?: {
    consumers: number;
    queues: number;
    exchanges: number;
  };
}

export interface QueueFiltros {
  page?: number;
  size?: number;
  name?: string;
}

export interface CreateQueueRequest {
  name: string;
  durable: boolean;
  exclusive: boolean;
  autoDelete: boolean;
}

export interface CreateExchangeRequest {
  name: string;
  type: 'direct' | 'topic' | 'fanout';
  durable: boolean;
}

export interface CreateBindingRequest {
  queueName: string;
  exchangeName: string;
  routingKey: string;
}
