import 'package:flutter/material.dart';

import '../models/models.dart';
import '../widgets/common.dart';
import 'result_screen.dart';

/// 4단계: 여행 기간 (일수)
class DaysScreen extends StatefulWidget {
  final TripDraft draft;

  const DaysScreen({super.key, required this.draft});

  @override
  State<DaysScreen> createState() => _DaysScreenState();
}

class _DaysScreenState extends State<DaysScreen> {
  static const int minDays = 2;
  static const int maxDays = 15;

  late int _days = widget.draft.tripDays.clamp(minDays, maxDays).toInt();

  void _next() {
    widget.draft.tripDays = _days;
    Navigator.push(context,
        MaterialPageRoute(builder: (_) => ResultScreen(draft: widget.draft)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(),
      body: ListView(
        children: [
          const StepHeader(
            step: '4 / 4',
            title: '며칠 동안 여행하세요?',
            subtitle: '첫날·마지막날은 공항 이동으로 반나절씩 빼고 계산해요',
          ),
          const SizedBox(height: 24),
          Center(
            child: Text('${_days - 1}박 $_days일',
                style: theme.textTheme.displaySmall
                    ?.copyWith(fontWeight: FontWeight.bold)),
          ),
          const SizedBox(height: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Slider(
              value: _days.toDouble(),
              min: minDays.toDouble(),
              max: maxDays.toDouble(),
              divisions: maxDays - minDays,
              label: '$_days일',
              onChanged: (v) => setState(() => _days = v.round()),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              alignment: WrapAlignment.center,
              children: [3, 4, 5, 7, 10].map((d) {
                return ChoiceChip(
                  label: Text('${d - 1}박 $d일'),
                  selected: _days == d,
                  onSelected: (_) => setState(() => _days = d),
                );
              }).toList(),
            ),
          ),
        ],
      ),
      bottomNavigationBar:
          BottomActionButton(label: '추천 동선 보기', onPressed: _next),
    );
  }
}
