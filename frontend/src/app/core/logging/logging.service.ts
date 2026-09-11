import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";

export enum LogLevel
{
    DEACTIVATED = 0,
    DEBUG = 1,
    INFO = 2,
    WARN = 3,
    ERROR = 4
}

export interface LogEntry
{
    timestamp: string;
    level: LogLevel;
    message: string;
    data?: any;
}

@Injectable({ providedIn: 'root' })
export class LoggingService
{
    private readonly minLevel: LogLevel;
    
    constructor()
    {
        this.minLevel = environment.logging.minLevel;
    }

    debug(message: string, data?: any): void
    {
        this.log(LogLevel.DEBUG, message, data);
    }

    info(message: string, data?: any): void
    {
        this.log(LogLevel.INFO, message, data);
    }

    warn(message: string, data?: any): void
    {
        this.log(LogLevel.WARN, message, data);
    }

    error(message: string, data?: any): void
    {
        this.log(LogLevel.ERROR, message, data);
    }

    private log(level: LogLevel, message: string, data?: any): void
    {
        if (this.minLevel === LogLevel.DEACTIVATED || level < this.minLevel)
            return;

        const entry: LogEntry =
        {
            timestamp: new Date().toISOString(),
            level,
            message,
            data
        }

        this.writeToConsole(entry);
    }

    private writeToConsole(entry: LogEntry): void
    {
        const levelName: string = LogLevel[entry.level]

        const prefix = `[${entry.timestamp}] [${levelName}]`;

        switch (entry.level)
        {
            case LogLevel.DEBUG:
                console.debug(prefix, entry.message, entry.data);
                break;
            
            case LogLevel.INFO:
                console.info(prefix, entry.message, entry.data);
                break;

            case LogLevel.WARN:
                console.warn(prefix, entry.message, entry.data);
                break;

            case LogLevel.ERROR:
                console.error(prefix, entry.message, entry.data);
                break;
        }
    }
}