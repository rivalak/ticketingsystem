package ticketingsystem;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

class SeatNode {
  private final int seatId;
  private AtomicLong availableSeat;

  public SeatNode(final int seatId) {
    this.seatId = seatId;
    this.availableSeat = new AtomicLong(0);
  }

  public int trySealTic(final int departure, final int arrival) {
    long oldAvailSeat = 0;
    long newAvailSeat = 0;
    long temp = 0;

    for (int i = departure-1; i < arrival-1; i++) {
      long pow = 1;
      pow = pow << i;
      temp |= pow;
    } 

    do {
      oldAvailSeat = this.availableSeat.get();//读
      long result = temp & oldAvailSeat;
      if (result != 0) {
        return -1;
      } 
      else {
        newAvailSeat = temp | oldAvailSeat;
      }
    } while (!this.availableSeat.compareAndSet(oldAvailSeat, newAvailSeat));//cas试图写

    return this.seatId;

  }

  public int inquiryTic(final int departure, final int arrival) {
    long oldAvailSeat = this.availableSeat.get();
    long temp = 0;
    long pow;

    for (int i = departure-1; i < arrival-1; i++) {
      pow = 1;
      pow = pow << i;
      temp |= pow;
    } 
    long result = temp & oldAvailSeat;

    return (result == 0) ? 1 : 0;

  }

  public boolean tryRefundTic(final int departure, final int arrival) {
    long oldAvailSeat = 0;
    long newAvailSeat = 0;
    long temp = 0;

    for (int i = departure-1; i < arrival-1; i++) {
      long pow = 1;
      pow = pow << i;
      temp |= pow;
    } 
    temp = ~temp;
    do {
      oldAvailSeat = this.availableSeat.get();
      newAvailSeat = temp & oldAvailSeat;
    } while (!this.availableSeat.compareAndSet(oldAvailSeat, newAvailSeat));

    return true;
  }

}

class CoachNode {
  private final int coachId;
  private final int seatNum;
  private ArrayList<SeatNode> seatList;

  public CoachNode(final int coachId, final int seatNum) {
    this.coachId = coachId;
    this.seatNum = seatNum;
    seatList = new ArrayList<SeatNode>(seatNum);

    for (int seatId = 1; seatId <= seatNum; seatId++)
      this.seatList.add(new SeatNode(seatId));
  }

  public Ticket trySealTic(final int departure, final int arrival) {
    Ticket ticket = new Ticket();
    //遍历所有seat
    int randSeat = ThreadLocalRandom.current().nextInt(this.seatNum);
    for (int i = 0; i < this.seatNum; i++) {
      int resultSeatId = this.seatList.get(randSeat).trySealTic(departure, arrival);
      if (resultSeatId != -1) {
        ticket.coach = this.coachId;
        ticket.seat = resultSeatId;
        return ticket;
      } 
      randSeat = (randSeat+1) % this.seatNum;
    } 
    return null;

  }

  public int inquiryTic(final int departure, final int arrival) {
    int ticSum = 0;
    for (int i = 0; i < this.seatNum; i++)
      ticSum += this.seatList.get(i).inquiryTic(departure, arrival);
    return ticSum;
  }

  public boolean tryRefundTic(final int seatId, final int departure, final int arrival) {
    return this.seatList.get(seatId-1).tryRefundTic(departure, arrival);
  }

}

class RouteNode {
  private final int routeId;
  private final int coachNum;
  private ArrayList<CoachNode> coachList;
  private AtomicLong ticketId;
  private Queue<Long> queue_SoldTicket;

  public RouteNode(final int routeId, final int coachNum, final int seatNum) {
    this.routeId = routeId;
    this.coachNum = coachNum;
    this.coachList = new ArrayList<CoachNode>(coachNum);
    this.ticketId = new AtomicLong(0);
    this.queue_SoldTicket = new ConcurrentLinkedQueue<Long>();

    for (int coachId = 1; coachId <= coachNum; coachId++)
      this.coachList.add(new CoachNode(coachId, seatNum));
  }

  public Ticket trySealTic(final String passenger, final int departure, final int arrival) {
    //遍历所有coach
    int randCoach = ThreadLocalRandom.current().nextInt(this.coachNum);
    for (int i = 0; i < this.coachNum; i++) {
      Ticket ticket = this.coachList.get(randCoach).trySealTic(departure, arrival);
      if (ticket != null) {
        ticket.tid = this.routeId*10000000 + this.ticketId.getAndIncrement();
        ticket.passenger = passenger;
        ticket.route = this.routeId;
        ticket.departure = departure;
        ticket.arrival = arrival;

        //每张车票hashCode
        long tic_hashCode = 0;
        tic_hashCode |= ticket.tid << 32;
        tic_hashCode |= ticket.coach << 24;
        tic_hashCode |= ticket.seat << 12;
        tic_hashCode |= ticket.departure << 6;
        tic_hashCode |= ticket.arrival;
        this.queue_SoldTicket.add(new Long(tic_hashCode));
        return ticket;

      } 

      randCoach = (randCoach+1) % this.coachNum;
    } 

    return null;
  }

  public int inquiryTic(final int departure, final int arrival) {
    int ticSum = 0;
    for (int i = 0; i < this.coachNum; i++) 
      ticSum += this.coachList.get(i).inquiryTic(departure, arrival);
    return ticSum;
  }

  public boolean tryRefundTic(final Ticket ticket) {
    long tic_hashCode = 0;
    tic_hashCode |= ticket.tid << 32;
    tic_hashCode |= ticket.coach << 24;
    tic_hashCode |= ticket.seat << 12;
    tic_hashCode |= ticket.departure << 6;
    tic_hashCode |= ticket.arrival;
    if (!this.queue_SoldTicket.contains(tic_hashCode)) 
      return false;
    else {
      this.queue_SoldTicket.remove(tic_hashCode);
      return this.coachList.get(ticket.coach-1).tryRefundTic(ticket.seat, ticket.departure, ticket.arrival);
    }
  }

}

public class TicketingDS implements TicketingSystem {
  private final int routeNum;
  private final int stationNum;
  private ArrayList<RouteNode> routeList;

  public TicketingDS(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) {
    this.routeNum = routeNum;
    this.stationNum = stationNum;

    this.routeList = new ArrayList<RouteNode>(routeNum);
    for (int routeId = 1; routeId <= routeNum; routeId++)//routeId从1开始
      this.routeList.add(new RouteNode(routeId, coachNum, seatNum));
  }

  public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
    if (route <=0 || route > this.routeNum || arrival > this.stationNum || departure >= arrival) 
      return null;
    return this.routeList.get(route-1).trySealTic(passenger, departure, arrival);
  }

  public int inquiry(int route, int departure, int arrival) {
    if (route <=0 || route > this.routeNum || arrival > this.stationNum || departure >= arrival) 
      return -1;
    return this.routeList.get(route-1).inquiryTic(departure, arrival);
  }

  public boolean refundTicket(Ticket ticket) {
    final int routeId = ticket.route;
    if (ticket == null || routeId <=0 || routeId > this.routeNum) 
      return false;
    return this.routeList.get(routeId-1).tryRefundTic(ticket);
  }

}
