import 'package:flutter/material.dart';

import '../models/models.dart';
import 'style_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              Icon(Icons.travel_explore_rounded,
                  size: 56, color: theme.colorScheme.primary),
              const SizedBox(height: 16),
              Text('어디로 떠나볼까요?',
                  style: theme.textTheme.headlineMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text('여행 스타일과 기간만 고르면\n가장 알맞은 동선을 짜드려요',
                  style: theme.textTheme.bodyLarge
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
              const Spacer(flex: 2),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: FilledButton.icon(
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('여행 플랜 만들기',
                      style: TextStyle(fontSize: 17)),
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (_) => StyleScreen(draft: TripDraft())),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
